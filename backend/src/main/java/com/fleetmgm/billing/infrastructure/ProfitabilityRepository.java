package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.vehicle.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Backs the profitability report (Hito 34 — "rentabilidad"). This is not a CRUD feature over
 * {@link Vehicle} — it extends the bare Spring Data {@link Repository} marker (not
 * {@code JpaRepository}) purely to get a queryable Spring Data repository bean for a single
 * custom aggregation query, without exposing (or duplicating) standard CRUD methods that
 * {@code VehicleRepository} already owns.
 * <p>
 * A static native query is required here: the formula in {@code planning.md}'s "Rentabilidad"
 * section aggregates across {@code invoice_line_items -> jobs}, {@code maintenance_records}, and
 * {@code supplier_invoices} per vehicle, none of which have a JPA relationship back to
 * {@link Vehicle} that a pure JPQL join could traverse in one query (correlated per-vehicle
 * subqueries are not expressible in JPQL). No caller-supplied value is concatenated into the SQL —
 * the only dynamic part is the standard Spring Data {@link Pageable}, which Spring appends safely
 * (LIMIT/OFFSET bind parameters), so this does not violate the "no dynamic native queries" rule —
 * that rule targets string-concatenated user input, not native queries per se.
 * <p>
 * Every revenue subquery below filters {@code inv.status IN ('ISSUED', 'PAID', 'OVERDUE')} — an
 * explicit allowlist, not a {@code <> 'CANCELLED'} denylist, so a future status added to
 * {@code InvoiceStatus} doesn't silently start counting as revenue. This matters because
 * {@code InvoiceService.delete()} cancels an ISSUED/OVERDUE invoice by flipping its status to
 * CANCELLED without setting {@code deletedAt} (the fiscal invoice number must stay visible for
 * audit — see that method's own comment), so {@code deleted_at IS NULL} alone is not enough to
 * exclude it here.
 */
public interface ProfitabilityRepository extends Repository<Vehicle, UUID> {

    @Query(value = """
            SELECT v.id AS vehicleId,
                   v.license_plate AS vehicleLicensePlate,
                   v.make AS vehicleMake,
                   v.model AS vehicleModel,
                   COALESCE((SELECT SUM(ili.subtotal)
                             FROM invoice_line_items ili
                             JOIN jobs j ON ili.linked_job_id = j.id
                             JOIN invoices inv ON ili.invoice_id = inv.id
                             WHERE j.vehicle_id = v.id
                               AND j.deleted_at IS NULL
                               AND inv.deleted_at IS NULL
                               AND inv.status IN ('ISSUED', 'PAID', 'OVERDUE')), 0) AS revenue,
                   COALESCE((SELECT SUM(mr.cost)
                             FROM maintenance_records mr
                             WHERE mr.vehicle_id = v.id
                               AND mr.deleted_at IS NULL), 0)
                   + COALESCE((SELECT SUM(si.total)
                               FROM supplier_invoices si
                               WHERE si.vehicle_id = v.id
                                 AND si.deleted_at IS NULL), 0)
                   + COALESCE((SELECT SUM(sili.subtotal)
                               FROM supplier_invoice_line_items sili
                               JOIN supplier_invoices si2 ON sili.invoice_id = si2.id
                               WHERE sili.vehicle_id = v.id
                                 AND si2.vehicle_id IS NULL
                                 AND si2.deleted_at IS NULL), 0) AS costs
            FROM vehicles v
            WHERE v.deleted_at IS NULL
            """,
            countQuery = "SELECT count(*) FROM vehicles v WHERE v.deleted_at IS NULL",
            nativeQuery = true)
    Page<VehicleProfitabilityProjection> findProfitabilityByVehicle(Pageable pageable);

    // Intentional duplication of the aggregation SQL above, scoped to a single vehicle instead of
    // a page. Not refactored into a shared SQL fragment/Java constant/PostgreSQL view — that's out
    // of scope for this slice. planning.md's "Rentabilidad" section already flags a PostgreSQL view
    // as a possible future alternative if this duplication becomes a maintenance problem.
    @Query(value = """
            SELECT v.id AS vehicleId,
                   v.license_plate AS vehicleLicensePlate,
                   v.make AS vehicleMake,
                   v.model AS vehicleModel,
                   COALESCE((SELECT SUM(ili.subtotal)
                             FROM invoice_line_items ili
                             JOIN jobs j ON ili.linked_job_id = j.id
                             JOIN invoices inv ON ili.invoice_id = inv.id
                             WHERE j.vehicle_id = v.id
                               AND j.deleted_at IS NULL
                               AND inv.deleted_at IS NULL
                               AND inv.status IN ('ISSUED', 'PAID', 'OVERDUE')), 0) AS revenue,
                   COALESCE((SELECT SUM(mr.cost)
                             FROM maintenance_records mr
                             WHERE mr.vehicle_id = v.id
                               AND mr.deleted_at IS NULL), 0)
                   + COALESCE((SELECT SUM(si.total)
                               FROM supplier_invoices si
                               WHERE si.vehicle_id = v.id
                                 AND si.deleted_at IS NULL), 0)
                   + COALESCE((SELECT SUM(sili.subtotal)
                               FROM supplier_invoice_line_items sili
                               JOIN supplier_invoices si2 ON sili.invoice_id = si2.id
                               WHERE sili.vehicle_id = v.id
                                 AND si2.vehicle_id IS NULL
                                 AND si2.deleted_at IS NULL), 0) AS costs
            FROM vehicles v
            WHERE v.id = :vehicleId
              AND v.deleted_at IS NULL
            """,
            nativeQuery = true)
    Optional<VehicleProfitabilityProjection> findProfitabilityByVehicleId(@Param("vehicleId") UUID vehicleId);

    /**
     * Fleet-wide (not per-vehicle) monthly revenue/costs trend backing the Dashboard's monthly
     * Ingresos/Gastos chart (Hito 43 redesign — replaces the earlier per-vehicle comparison chart,
     * which is now reserved for a future Vehicles-admin-table piece via
     * {@link #findProfitabilityByVehicle}).
     * <p>
     * {@code generate_series} builds the month spine so every month in [from, to] appears in the
     * result even with zero activity (COALESCE guards the two LEFT JOINs) — the chart's X-axis
     * needs every month present for the 3/6/12-month selector to render consistently. Revenue is
     * {@code invoices.subtotal} directly, summed per month of {@code issue_date} — matching Spanish
     * IVA law's accrual criterion ("criterio de devengo": revenue is recognised at issuance,
     * regardless of payment status), the standard most invoicing tools use. Deliberately NOT the
     * {@code invoice_line_items -> jobs} formula {@link #findProfitabilityByVehicle} uses — that
     * join exists there only to attribute revenue to a specific vehicle, and excluding a line item
     * with no {@code linked_job_id} (e.g. a manually-entered line with no Job) would silently drop
     * real invoiced revenue from this fleet-wide total, which has no per-vehicle attribution to
     * make in the first place. Costs reuses the same two sources as
     * {@code DashboardService.monthlyCosts()} (maintenance_records.cost + supplier_invoices.total)
     * — not the per-vehicle query's extra supplier_invoice_line_items source, which only exists to
     * split a single supplier invoice across multiple vehicles and has no bearing on the fleet-wide
     * total.
     * <p>
     * {@code :from}/{@code :to} are the only dynamic parts, bound via Spring Data (no string
     * concatenation), so this does not violate the "no dynamic native queries" rule.
     */
    @Query(value = """
            SELECT to_char(month_series, 'YYYY-MM') AS month,
                   COALESCE(rev.revenue, 0) AS revenue,
                   COALESCE(cost.costs, 0) AS costs
            FROM generate_series(date_trunc('month', CAST(:from AS date)), date_trunc('month', CAST(:to AS date)), interval '1 month') AS month_series
            LEFT JOIN (
                SELECT date_trunc('month', inv.issue_date) AS month, SUM(inv.subtotal) AS revenue
                FROM invoices inv
                WHERE inv.deleted_at IS NULL AND inv.issue_date IS NOT NULL
                  AND inv.status IN ('ISSUED', 'PAID', 'OVERDUE')
                GROUP BY date_trunc('month', inv.issue_date)
            ) rev ON rev.month = month_series
            LEFT JOIN (
                SELECT month, SUM(amount) AS costs FROM (
                    SELECT date_trunc('month', workshop_entry_date) AS month, cost AS amount
                    FROM maintenance_records
                    WHERE deleted_at IS NULL AND workshop_entry_date IS NOT NULL AND cost IS NOT NULL
                    UNION ALL
                    SELECT date_trunc('month', invoice_date) AS month, total AS amount
                    FROM supplier_invoices
                    WHERE deleted_at IS NULL
                ) combined
                GROUP BY month
            ) cost ON cost.month = month_series
            ORDER BY month_series
            """,
            nativeQuery = true)
    List<MonthlyFinancialProjection> findMonthlyFinancialTrend(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

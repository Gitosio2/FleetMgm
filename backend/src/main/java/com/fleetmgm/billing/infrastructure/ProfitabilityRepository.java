package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.vehicle.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

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
                               AND inv.deleted_at IS NULL), 0) AS revenue,
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
}

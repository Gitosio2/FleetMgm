package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, UUID> {

    // List query denormalizes vehicle and supplier fields into SupplierInvoiceResponse — JOIN FETCH
    // avoids N+1 (CLAUDE.md JPA rule). vehicle is nullable (LEFT join); supplier is a mandatory
    // relation like Invoice.client, so it uses a plain JOIN FETCH. Both filters are optional — the
    // standard "(:param IS NULL OR ...)" JPQL idiom keeps the query fully parameterized (no string
    // concatenation), satisfying the dynamic-query SQL injection rule while still supporting
    // Pageable on a to-one join.
    // Pure chronological order, newest first — status does not influence sort (matches the
    // list-view convention of QuickBooks/Xero/Stripe Invoicing: status is a filterable column,
    // not a sort bucket; see InvoiceRepository.findAllJoinFetch for the same change on the
    // client-invoice side). invoiceDate is NOT NULL for every row (no DRAFT concept here), so
    // unlike Invoice it's a safe primary sort key directly. category is the secondary key so
    // same-date invoices group by expense type in the list view. supplierInvoiceNumber comes next
    // as a human-meaningful tertiary key — but it's nullable and has no unique constraint (V7), so
    // it CANNOT be trusted alone to break ties: two rows can share a number, or both be null, which
    // would reproduce the exact non-deterministic-pagination bug fixed above (createdAt DESC, id
    // DESC). Those two stay as the true final tiebreak beneath supplierInvoiceNumber, guaranteeing
    // every row has a fully deterministic position regardless of what invoiceNumber/category data
    // looks like. Callers must pass an unsorted Pageable.
    // supplierId/status and the four date/amount ranges are newer than vehicleId/category and use
    // CAST(:param AS string) IS NULL instead of a bare ":param IS NULL" — see AuditLogRepository's
    // comment on the same idiom: a parameter that appears ONLY in a bare IS NULL check gives
    // Postgres no type context to infer from, even for enums, and fails 500 on every request that
    // leaves it null. vehicleId/category predate this fix and are left as-is since they already work.
    @Query("SELECT si FROM SupplierInvoice si JOIN FETCH si.supplier LEFT JOIN FETCH si.vehicle "
            + "WHERE (:vehicleId IS NULL OR si.vehicle.id = :vehicleId) "
            + "AND (:category IS NULL OR si.category = :category) "
            + "AND (CAST(:supplierId AS string) IS NULL OR si.supplier.id = :supplierId) "
            + "AND (CAST(:status AS string) IS NULL OR si.status = :status) "
            + "AND (CAST(:invoiceDateFrom AS string) IS NULL OR si.invoiceDate >= :invoiceDateFrom) "
            + "AND (CAST(:invoiceDateTo AS string) IS NULL OR si.invoiceDate <= :invoiceDateTo) "
            + "AND (CAST(:dueDateFrom AS string) IS NULL OR si.dueDate >= :dueDateFrom) "
            + "AND (CAST(:dueDateTo AS string) IS NULL OR si.dueDate <= :dueDateTo) "
            + "AND (CAST(:totalMin AS string) IS NULL OR si.total >= :totalMin) "
            + "AND (CAST(:totalMax AS string) IS NULL OR si.total <= :totalMax) "
            + "ORDER BY si.invoiceDate DESC, si.category ASC, si.supplierInvoiceNumber ASC, "
            + "si.createdAt DESC, si.id DESC")
    Page<SupplierInvoice> findAllJoinFetch(
            @Param("vehicleId") UUID vehicleId,
            @Param("category") ExpenseCategory category,
            @Param("supplierId") UUID supplierId,
            @Param("status") SupplierInvoiceStatus status,
            @Param("invoiceDateFrom") LocalDate invoiceDateFrom,
            @Param("invoiceDateTo") LocalDate invoiceDateTo,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            @Param("totalMin") BigDecimal totalMin,
            @Param("totalMax") BigDecimal totalMax,
            Pageable pageable);

    // Fleet-summary KPI (dashboard) — monthly supplier costs, summed alongside
    // MaintenanceRepository.sumCostByWorkshopEntryDateBetween. COALESCE guards against SUM
    // returning null when no rows fall in the range.
    @Query("SELECT COALESCE(SUM(si.total), 0) FROM SupplierInvoice si WHERE si.invoiceDate BETWEEN :from AND :to")
    BigDecimal sumTotalByInvoiceDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Financial-summary KPI (dashboard) — top-N unpaid supplier invoices due soon. No lower bound
    // on dueDate: already-overdue rows (dueDate in the past) must be included too, not filtered
    // out — the frontend flags them separately (see DashboardService.getFinancialSummary()).
    // SupplierInvoiceStatus has no OVERDUE value at all (just PENDING/PAID), so "overdue" is
    // always computed live from dueDate, same as the client-invoice side.
    @Query("SELECT si FROM SupplierInvoice si JOIN FETCH si.supplier "
            + "WHERE si.status = com.fleetmgm.billing.domain.SupplierInvoiceStatus.PENDING AND si.dueDate <= :to "
            + "ORDER BY si.dueDate ASC")
    List<SupplierInvoice> findUpcomingPayables(@Param("to") LocalDate to, Pageable pageable);
}

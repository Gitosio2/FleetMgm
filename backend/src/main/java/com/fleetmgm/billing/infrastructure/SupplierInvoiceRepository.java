package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
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
    // PENDING sorts before PAID, newest invoiceDate first within each group — invoiceDate is
    // NOT NULL for every row (no DRAFT concept here), so unlike Invoice it's a safe sort key
    // directly, no createdAt fallback needed. Callers must pass an unsorted Pageable.
    @Query("SELECT si FROM SupplierInvoice si JOIN FETCH si.supplier LEFT JOIN FETCH si.vehicle "
            + "WHERE (:vehicleId IS NULL OR si.vehicle.id = :vehicleId) "
            + "AND (:category IS NULL OR si.category = :category) "
            + "ORDER BY CASE WHEN si.status = com.fleetmgm.billing.domain.SupplierInvoiceStatus.PAID THEN 1 ELSE 0 END, "
            + "si.invoiceDate DESC")
    Page<SupplierInvoice> findAllJoinFetch(
            @Param("vehicleId") UUID vehicleId, @Param("category") ExpenseCategory category, Pageable pageable);

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

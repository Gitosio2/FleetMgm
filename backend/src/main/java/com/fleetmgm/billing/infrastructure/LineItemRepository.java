package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    // Used by InvoiceService.issue() to sum all of an invoice's line items into its
    // subtotal/taxAmount/total. No JOIN FETCH needed — issue() only reads the subtotal column.
    List<InvoiceLineItem> findAllByInvoiceId(UUID invoiceId);

    // Used by InvoiceService.list() to batch-fetch line items for an entire page of invoices in
    // a single query, then group them in memory by invoice id — avoids the N+1 that a per-invoice
    // findAllByInvoiceId() call inside the page-mapping loop would introduce (CLAUDE.md JPA rule).
    List<InvoiceLineItem> findAllByInvoiceIdIn(List<UUID> invoiceIds);

    // JPQL, not native — unlike ProfitabilityRepository's aggregate query, linkedJob.vehicle is a
    // real JPA path here, and Invoice's @SQLRestriction(deleted_at IS NULL) applies automatically
    // through the join.
    @Query("SELECT li FROM InvoiceLineItem li JOIN FETCH li.invoice JOIN li.linkedJob j "
            + "WHERE j.vehicle.id = :vehicleId "
            + "AND (:year IS NULL OR EXTRACT(YEAR FROM li.invoice.issueDate) = :year) "
            + "AND (:month IS NULL OR EXTRACT(MONTH FROM li.invoice.issueDate) = :month) "
            + "ORDER BY li.invoice.issueDate DESC")
    List<InvoiceLineItem> findAllByVehicleIdAndPeriod(
            @Param("vehicleId") UUID vehicleId,
            @Param("year") Integer year,
            @Param("month") Integer month);
}

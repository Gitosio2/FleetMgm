package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    // Used by InvoiceService.issue() to sum all of an invoice's line items into its
    // subtotal/taxAmount/total. No JOIN FETCH needed — issue() only reads the subtotal column.
    List<InvoiceLineItem> findAllByInvoiceId(UUID invoiceId);

    // Used by InvoiceService.updateLineItem() to verify the line item actually belongs to the
    // invoice in the URL path, not just that some line item with that id exists anywhere — an
    // id-only findById() would let a caller edit any line item on any invoice by id alone (IDOR,
    // CLAUDE.md OWASP B).
    Optional<InvoiceLineItem> findByIdAndInvoiceId(UUID id, UUID invoiceId);

    // Used by InvoiceService.list() to batch-fetch line items for an entire page of invoices in
    // a single query, then group them in memory by invoice id — avoids the N+1 that a per-invoice
    // findAllByInvoiceId() call inside the page-mapping loop would introduce (CLAUDE.md JPA rule).
    List<InvoiceLineItem> findAllByInvoiceIdIn(List<UUID> invoiceIds);

    // JPQL, not native — unlike ProfitabilityRepository's aggregate query, linkedJob.vehicle is a
    // real JPA path here, and Invoice's @SQLRestriction(deleted_at IS NULL) applies automatically
    // through the join. issueDateFrom/issueDateTo replace the old year/month EXTRACT-based filter
    // (optional Desde/Hasta range, same "(CAST(:param AS string) IS NULL OR ...)" idiom as
    // InvoiceRepository's issueDateFrom/issueDateTo). The li.invoice.status allowlist mirrors
    // ProfitabilityRepository.findProfitabilityByVehicle/findProfitabilityByVehicleId — an explicit
    // ISSUED/PAID/OVERDUE allowlist, not a "<> CANCELLED" denylist, so a future InvoiceStatus value
    // doesn't silently start counting as revenue here either. This was previously missing: a
    // CANCELLED invoice's line items used to still show up in this vehicle revenue history.
    @Query("SELECT li FROM InvoiceLineItem li JOIN FETCH li.invoice JOIN li.linkedJob j "
            + "WHERE j.vehicle.id = :vehicleId "
            + "AND li.invoice.status IN (com.fleetmgm.billing.domain.InvoiceStatus.ISSUED, "
            + "     com.fleetmgm.billing.domain.InvoiceStatus.PAID, com.fleetmgm.billing.domain.InvoiceStatus.OVERDUE) "
            + "AND (CAST(:issueDateFrom AS string) IS NULL OR li.invoice.issueDate >= :issueDateFrom) "
            + "AND (CAST(:issueDateTo AS string) IS NULL OR li.invoice.issueDate <= :issueDateTo) "
            + "ORDER BY li.invoice.issueDate DESC")
    List<InvoiceLineItem> findAllByVehicleIdAndPeriod(
            @Param("vehicleId") UUID vehicleId,
            @Param("issueDateFrom") LocalDate issueDateFrom,
            @Param("issueDateTo") LocalDate issueDateTo);
}

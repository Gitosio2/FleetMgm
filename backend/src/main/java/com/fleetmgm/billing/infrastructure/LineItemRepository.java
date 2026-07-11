package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    // Used by InvoiceService.issue() to sum all of an invoice's line items into its
    // subtotal/taxAmount/total. No JOIN FETCH needed — issue() only reads the subtotal column.
    List<InvoiceLineItem> findAllByInvoiceId(UUID invoiceId);
}

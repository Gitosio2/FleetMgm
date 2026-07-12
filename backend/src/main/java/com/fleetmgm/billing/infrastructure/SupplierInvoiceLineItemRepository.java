package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierInvoiceLineItemRepository extends JpaRepository<SupplierInvoiceLineItem, UUID> {

    // Used by SupplierInvoiceService.getById(), update() (mutual-exclusion check), and pay()
    // (reconciliation check) to fetch a single invoice's line items. No JOIN FETCH needed — every
    // caller only reads scalar columns (subtotal, vehicle id), never dereferences a lazy
    // association after the transaction closes.
    List<SupplierInvoiceLineItem> findAllByInvoiceId(UUID invoiceId);

    // Used by SupplierInvoiceService.list() to batch-fetch line items for an entire page of
    // invoices in a single query, then group them in memory by invoice id — avoids the N+1 that a
    // per-invoice findAllByInvoiceId() call inside the page-mapping loop would introduce
    // (CLAUDE.md JPA rule).
    List<SupplierInvoiceLineItem> findAllByInvoiceIdIn(List<UUID> invoiceIds);

    // Ownership-safe lookup for updateLineItem()/deleteLineItem() — scopes the id lookup to the
    // invoice id also present in the URL, so a caller cannot mutate a line item that exists but
    // belongs to a different invoice (OWASP B / IDOR defense per CLAUDE.md).
    Optional<SupplierInvoiceLineItem> findByIdAndInvoiceId(UUID id, UUID invoiceId);
}

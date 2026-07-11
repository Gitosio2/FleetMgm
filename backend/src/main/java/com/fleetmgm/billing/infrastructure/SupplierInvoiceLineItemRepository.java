package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Unlike LineItemRepository (Invoice), SupplierInvoiceService.addLineItem() never sums existing
// lines back into the invoice's own totals (no issue()-equivalent step — see SupplierInvoice's
// design notes), so no findAllByInvoiceId(...) query is needed here. Plain CRUD via save() is
// the entire contract this feature requires.
public interface SupplierInvoiceLineItemRepository extends JpaRepository<SupplierInvoiceLineItem, UUID> {
}

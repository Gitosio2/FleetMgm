package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // Vehicle profitability panel's merged "Historial de gastos" list (Hito 45) — the split-invoice
    // complement to SupplierInvoiceRepository.findAllByVehicleIdAndPeriod. "inv.vehicle IS NULL" is
    // the deliberate double-count guard: a line item whose parent invoice ALSO has a header-level
    // vehicle is already counted whole via the other query, mirroring ProfitabilityRepository's
    // "si2.vehicle_id IS NULL" subquery condition exactly. Do not drop this condition — dropping it
    // would double-count against ProfitabilityService.getExpensesByVehicle/"Totales" -> "Gastos".
    @Query("SELECT sili FROM SupplierInvoiceLineItem sili JOIN FETCH sili.invoice inv JOIN FETCH inv.supplier "
            + "WHERE sili.vehicle.id = :vehicleId AND inv.vehicle IS NULL "
            + "AND (CAST(:invoiceDateFrom AS string) IS NULL OR inv.invoiceDate >= :invoiceDateFrom) "
            + "AND (CAST(:invoiceDateTo AS string) IS NULL OR inv.invoiceDate <= :invoiceDateTo) "
            + "ORDER BY inv.invoiceDate DESC")
    List<SupplierInvoiceLineItem> findAllByVehicleIdAndPeriod(@Param("vehicleId") UUID vehicleId,
            @Param("invoiceDateFrom") LocalDate invoiceDateFrom, @Param("invoiceDateTo") LocalDate invoiceDateTo);
}

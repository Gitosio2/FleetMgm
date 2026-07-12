package com.fleetmgm.billing.api;

import com.fleetmgm.billing.application.SupplierInvoiceService;
import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.dto.CreateSupplierInvoiceRequest;
import com.fleetmgm.billing.dto.PayInvoiceRequest;
import com.fleetmgm.billing.dto.SupplierInvoiceResponse;
import com.fleetmgm.billing.dto.SupplierLineItemRequest;
import com.fleetmgm.billing.dto.SupplierLineItemResponse;
import com.fleetmgm.billing.dto.UpdateSupplierInvoiceRequest;
import com.fleetmgm.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supplier-invoices")
public class SupplierInvoiceController {

    private final SupplierInvoiceService supplierInvoiceService;

    public SupplierInvoiceController(SupplierInvoiceService supplierInvoiceService) {
        this.supplierInvoiceService = supplierInvoiceService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SupplierInvoiceResponse>> list(
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) ExpenseCategory category,
            @PageableDefault(size = 20, sort = "invoiceDate") Pageable pageable) {
        return ResponseEntity.ok(supplierInvoiceService.list(vehicleId, category, pageable));
    }

    @PostMapping
    public ResponseEntity<SupplierInvoiceResponse> create(
            @Valid @RequestBody CreateSupplierInvoiceRequest request,
            UriComponentsBuilder uriBuilder) {
        SupplierInvoiceResponse response = supplierInvoiceService.create(request);
        URI location = uriBuilder.path("/api/v1/supplier-invoices/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierInvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierInvoiceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierInvoiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierInvoiceRequest request) {
        return ResponseEntity.ok(supplierInvoiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        supplierInvoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<SupplierInvoiceResponse> pay(
            @PathVariable UUID id,
            @RequestBody(required = false) PayInvoiceRequest request) {
        return ResponseEntity.ok(supplierInvoiceService.pay(id, request));
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<SupplierLineItemResponse> addLineItem(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierLineItemRequest request) {
        SupplierLineItemResponse response = supplierInvoiceService.addLineItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{invoiceId}/line-items/{lineItemId}")
    public ResponseEntity<SupplierLineItemResponse> updateLineItem(
            @PathVariable UUID invoiceId, @PathVariable UUID lineItemId,
            @Valid @RequestBody SupplierLineItemRequest request) {
        return ResponseEntity.ok(supplierInvoiceService.updateLineItem(invoiceId, lineItemId, request));
    }

    @DeleteMapping("/{invoiceId}/line-items/{lineItemId}")
    public ResponseEntity<Void> deleteLineItem(@PathVariable UUID invoiceId, @PathVariable UUID lineItemId) {
        supplierInvoiceService.deleteLineItem(invoiceId, lineItemId);
        return ResponseEntity.noContent().build();
    }
}

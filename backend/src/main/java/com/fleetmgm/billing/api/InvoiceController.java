package com.fleetmgm.billing.api;

import com.fleetmgm.billing.application.InvoiceService;
import com.fleetmgm.billing.dto.CreateInvoiceRequest;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemRequest;
import com.fleetmgm.billing.dto.LineItemResponse;
import com.fleetmgm.billing.dto.PayInvoiceRequest;
import com.fleetmgm.billing.dto.UpdateInvoiceRequest;
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
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<InvoiceResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            UriComponentsBuilder uriBuilder) {
        InvoiceResponse response = invoiceService.create(request);
        URI location = uriBuilder.path("/api/v1/invoices/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.issue(id));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> pay(
            @PathVariable UUID id,
            @RequestBody(required = false) PayInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.pay(id, request));
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<LineItemResponse> addLineItem(
            @PathVariable UUID id,
            @Valid @RequestBody LineItemRequest request) {
        LineItemResponse response = invoiceService.addLineItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package com.fleetmgm.supplier.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.supplier.application.SupplierService;
import com.fleetmgm.supplier.dto.CreateSupplierRequest;
import com.fleetmgm.supplier.dto.SupplierResponse;
import com.fleetmgm.supplier.dto.UpdateSupplierRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SupplierResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String taxId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(supplierService.list(name, taxId, pageable));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody CreateSupplierRequest request,
            UriComponentsBuilder uriBuilder) {
        SupplierResponse response = supplierService.create(request);
        URI location = uriBuilder.path("/api/v1/suppliers/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

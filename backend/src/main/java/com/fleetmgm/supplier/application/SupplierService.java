package com.fleetmgm.supplier.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.supplier.dto.CreateSupplierRequest;
import com.fleetmgm.supplier.dto.SupplierMapper;
import com.fleetmgm.supplier.dto.SupplierResponse;
import com.fleetmgm.supplier.dto.UpdateSupplierRequest;
import com.fleetmgm.supplier.infrastructure.SupplierRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public PageResponse<SupplierResponse> list(Pageable pageable) {
        return PageResponse.from(supplierRepository.findAll(pageable).map(supplierMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public SupplierResponse create(CreateSupplierRequest request) {
        assertTaxIdAvailable(request.taxId());
        Supplier supplier = supplierMapper.toEntity(request);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public SupplierResponse getById(UUID id) {
        return supplierRepository.findById(id)
                .map(supplierMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public SupplierResponse update(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier " + id + " not found"));
        if (request.taxId() != null && !request.taxId().isBlank()
                && supplierRepository.existsByTaxIdAndIdNot(request.taxId(), id)) {
            throw new ConflictException("SUPPLIER_TAX_ID_CONFLICT",
                    "A supplier with taxId " + request.taxId() + " already exists");
        }
        supplierMapper.updateEntity(request, supplier);
        return supplierMapper.toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier " + id + " not found"));
        supplier.setDeletedAt(Instant.now());
        supplierRepository.save(supplier);
    }

    private void assertTaxIdAvailable(String taxId) {
        if (taxId != null && !taxId.isBlank() && supplierRepository.existsByTaxId(taxId)) {
            throw new ConflictException("SUPPLIER_TAX_ID_CONFLICT",
                    "A supplier with taxId " + taxId + " already exists");
        }
    }
}

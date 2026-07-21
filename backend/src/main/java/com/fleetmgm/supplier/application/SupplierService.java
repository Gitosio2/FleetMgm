package com.fleetmgm.supplier.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
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

    private static final String ENTITY_TYPE = "Supplier";

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final AuditLogHelper auditLogHelper;

    public SupplierService(SupplierRepository supplierRepository, SupplierMapper supplierMapper, AuditLogHelper auditLogHelper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
        this.auditLogHelper = auditLogHelper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public PageResponse<SupplierResponse> list(String name, String taxId, Pageable pageable) {
        return PageResponse.from(supplierRepository.search(name, taxId, pageable).map(supplierMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public SupplierResponse create(CreateSupplierRequest request) {
        assertTaxIdAvailable(request.taxId());
        Supplier supplier = supplierMapper.toEntity(request);
        var saved = supplierMapper.toResponse(supplierRepository.save(supplier));
        auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.CREATE);
        return saved;
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
        var saved = supplierMapper.toResponse(supplierRepository.save(supplier));
        auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.UPDATE);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier " + id + " not found"));
        supplier.setDeletedAt(Instant.now());
        supplierRepository.save(supplier);
        auditLogHelper.log(ENTITY_TYPE, id.toString(), AuditAction.DELETE);
    }

    private void assertTaxIdAvailable(String taxId) {
        if (taxId != null && !taxId.isBlank() && supplierRepository.existsByTaxId(taxId)) {
            throw new ConflictException("SUPPLIER_TAX_ID_CONFLICT",
                    "A supplier with taxId " + taxId + " already exists");
        }
    }
}

package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.dto.CreateSupplierInvoiceRequest;
import com.fleetmgm.billing.dto.PayInvoiceRequest;
import com.fleetmgm.billing.dto.SupplierInvoiceResponse;
import com.fleetmgm.billing.dto.SupplierLineItemRequest;
import com.fleetmgm.billing.dto.SupplierLineItemResponse;
import com.fleetmgm.billing.dto.UpdateSupplierInvoiceRequest;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierInvoiceService {

    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<SupplierInvoiceResponse> list(UUID vehicleId, ExpenseCategory category, Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse create(CreateSupplierInvoiceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse getById(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse update(UUID id, UpdateSupplierInvoiceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse pay(UUID id, PayInvoiceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierLineItemResponse addLineItem(UUID invoiceId, SupplierLineItemRequest request) {
        throw new UnsupportedOperationException("Pending Hito 33");
    }
}

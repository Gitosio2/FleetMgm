package com.fleetmgm.billing.application;

import com.fleetmgm.billing.dto.CreateInvoiceRequest;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemRequest;
import com.fleetmgm.billing.dto.LineItemResponse;
import com.fleetmgm.billing.dto.UpdateInvoiceRequest;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InvoiceService {

    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<InvoiceResponse> list(Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse create(CreateInvoiceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public InvoiceResponse getById(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse update(UUID id, UpdateInvoiceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse issue(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse pay(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }

    @Transactional
    @PreAuthorize(ROLES)
    public LineItemResponse addLineItem(UUID invoiceId, LineItemRequest request) {
        throw new UnsupportedOperationException("Pending Hito 31");
    }
}

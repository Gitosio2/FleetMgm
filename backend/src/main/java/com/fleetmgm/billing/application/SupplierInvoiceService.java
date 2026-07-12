package com.fleetmgm.billing.application;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import com.fleetmgm.billing.domain.SupplierInvoiceStatus;
import com.fleetmgm.billing.dto.CreateSupplierInvoiceRequest;
import com.fleetmgm.billing.dto.PayInvoiceRequest;
import com.fleetmgm.billing.dto.SupplierInvoiceMapper;
import com.fleetmgm.billing.dto.SupplierInvoiceResponse;
import com.fleetmgm.billing.dto.SupplierLineItemRequest;
import com.fleetmgm.billing.dto.SupplierLineItemResponse;
import com.fleetmgm.billing.dto.UpdateSupplierInvoiceRequest;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceLineItemRepository;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.supplier.infrastructure.SupplierRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class SupplierInvoiceService {

    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    // Same 2-decimal HALF_UP currency rounding convention already established in InvoiceService —
    // applied here from the start so addLineItem() doesn't repeat the raw-scale bug that had to be
    // fixed later in Invoice's Hito 31 review.
    private static final int MONEY_SCALE = 2;

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierInvoiceMapper supplierInvoiceMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public SupplierInvoiceService(SupplierInvoiceRepository supplierInvoiceRepository,
                                   SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository,
                                   VehicleRepository vehicleRepository,
                                   MaintenanceRepository maintenanceRepository,
                                   SupplierRepository supplierRepository,
                                   SupplierInvoiceMapper supplierInvoiceMapper,
                                   AuditLogRepository auditLogRepository,
                                   UserRepository userRepository) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierInvoiceLineItemRepository = supplierInvoiceLineItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.supplierRepository = supplierRepository;
        this.supplierInvoiceMapper = supplierInvoiceMapper;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<SupplierInvoiceResponse> list(UUID vehicleId, ExpenseCategory category, Pageable pageable) {
        return PageResponse.from(supplierInvoiceRepository.findAllJoinFetch(vehicleId, category, pageable)
                .map(supplierInvoiceMapper::toResponse));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse create(CreateSupplierInvoiceRequest request) {
        Supplier supplier = resolveSupplier(request.supplierId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        SupplierInvoice invoice = supplierInvoiceMapper.toEntity(request);
        invoice.setSupplier(supplier);
        invoice.setVehicle(vehicle);
        return supplierInvoiceMapper.toResponse(supplierInvoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse getById(UUID id) {
        return supplierInvoiceMapper.toResponse(findInvoiceOrThrow(id));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse update(UUID id, UpdateSupplierInvoiceRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + id + " cannot be updated from state " + invoice.getStatus());
        Supplier supplier = resolveSupplier(request.supplierId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        supplierInvoiceMapper.updateEntity(request, invoice);
        invoice.setSupplier(supplier);
        invoice.setVehicle(vehicle);
        return supplierInvoiceMapper.toResponse(supplierInvoiceRepository.save(invoice));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);
        if (invoice.getStatus() != SupplierInvoiceStatus.PENDING) {
            throw new ConflictException("SUPPLIER_INVOICE_DELETE_NOT_ALLOWED",
                    "Supplier invoice " + id + " cannot be deleted from state " + invoice.getStatus());
        }
        invoice.setDeletedAt(Instant.now());
        supplierInvoiceRepository.save(invoice);
        auditDeletion(invoice);
    }

    private void auditDeletion(SupplierInvoice invoice) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog log = new AuditLog();
        log.setEntityType("SupplierInvoice");
        log.setEntityId(invoice.getId().toString());
        log.setAction(AuditAction.DELETE);
        log.setPerformedByEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> log.setPerformedByUserId(user.getId()));
        log.setPerformedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse pay(UUID id, PayInvoiceRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);
        if (invoice.getStatus() != SupplierInvoiceStatus.PENDING) {
            throw new ConflictException("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                    "Supplier invoice " + id + " cannot be paid from state " + invoice.getStatus());
        }
        invoice.setStatus(SupplierInvoiceStatus.PAID);
        invoice.setPaymentDate(request != null && request.paymentDate() != null
                ? request.paymentDate() : LocalDate.now());
        return supplierInvoiceMapper.toResponse(supplierInvoiceRepository.save(invoice));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierLineItemResponse addLineItem(UUID invoiceId, SupplierLineItemRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + invoiceId + " cannot receive line items from state " + invoice.getStatus());
        Vehicle linkedVehicle = resolveVehicle(request.vehicleId());
        MaintenanceRecord linkedMaintenance = resolveMaintenance(request.maintenanceRecordId());

        SupplierInvoiceLineItem lineItem = supplierInvoiceMapper.toEntity(request);
        lineItem.setInvoice(invoice);
        lineItem.setVehicle(linkedVehicle);
        lineItem.setMaintenanceRecord(linkedMaintenance);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return supplierInvoiceMapper.toResponse(supplierInvoiceLineItemRepository.save(lineItem));
    }

    // --- relation resolution helpers ---

    private SupplierInvoice findInvoiceOrThrow(UUID id) {
        return supplierInvoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_INVOICE_NOT_FOUND",
                        "Supplier invoice " + id + " not found"));
    }

    private void assertIsPending(SupplierInvoice invoice, String code, String message) {
        if (invoice.getStatus() != SupplierInvoiceStatus.PENDING) {
            throw new ConflictException(code, message);
        }
    }

    // Mandatory relation, unlike resolveVehicle/resolveMaintenance below — no null-check bypass,
    // supplierId is always @NotNull on the request.
    private Supplier resolveSupplier(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier " + supplierId + " not found"));
    }

    private Vehicle resolveVehicle(UUID vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " not found"));
    }

    private MaintenanceRecord resolveMaintenance(UUID maintenanceId) {
        if (maintenanceId == null) {
            return null;
        }
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + maintenanceId + " not found"));
    }
}

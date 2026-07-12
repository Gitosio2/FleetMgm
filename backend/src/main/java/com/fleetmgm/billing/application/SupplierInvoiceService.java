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
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final SupplierInvoiceMapper supplierInvoiceMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public SupplierInvoiceService(SupplierInvoiceRepository supplierInvoiceRepository,
                                   SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository,
                                   VehicleRepository vehicleRepository,
                                   MaintenanceRepository maintenanceRepository,
                                   SupplierInvoiceMapper supplierInvoiceMapper,
                                   AuditLogRepository auditLogRepository,
                                   UserRepository userRepository) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierInvoiceLineItemRepository = supplierInvoiceLineItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.supplierInvoiceMapper = supplierInvoiceMapper;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<SupplierInvoiceResponse> list(UUID vehicleId, ExpenseCategory category, Pageable pageable) {
        Page<SupplierInvoice> page = supplierInvoiceRepository.findAllJoinFetch(vehicleId, category, pageable);
        List<UUID> invoiceIds = page.getContent().stream().map(SupplierInvoice::getId).toList();
        // Single batched query for the whole page — grouping in memory here, instead of calling
        // supplierInvoiceLineItemRepository.findAllByInvoiceId() once per invoice inside the loop
        // below, is what keeps this at 2 queries total instead of 1+N (CLAUDE.md JPA N+1 rule).
        Map<UUID, List<SupplierInvoiceLineItem>> lineItemsByInvoiceId = supplierInvoiceLineItemRepository
                .findAllByInvoiceIdIn(invoiceIds)
                .stream()
                .collect(Collectors.groupingBy(lineItem -> lineItem.getInvoice().getId()));
        return PageResponse.from(page.map(invoice ->
                toResponseWithLineItems(invoice, lineItemsByInvoiceId.getOrDefault(invoice.getId(), List.of()))));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse create(CreateSupplierInvoiceRequest request) {
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        SupplierInvoice invoice = supplierInvoiceMapper.toEntity(request);
        invoice.setVehicle(vehicle);
        // A brand-new invoice can't have pre-existing line items (they're only added afterward via
        // addLineItem()), so there's nothing to fetch here - skip the query entirely.
        return toResponseWithLineItems(supplierInvoiceRepository.save(invoice), List.of());
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse getById(UUID id) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);
        List<SupplierInvoiceLineItem> lineItems = supplierInvoiceLineItemRepository.findAllByInvoiceId(id);
        return toResponseWithLineItems(invoice, lineItems);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierInvoiceResponse update(UUID id, UpdateSupplierInvoiceRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(id);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + id + " cannot be updated from state " + invoice.getStatus());
        List<SupplierInvoiceLineItem> existingLineItems = supplierInvoiceLineItemRepository.findAllByInvoiceId(id);
        if (request.vehicleId() != null && !existingLineItems.isEmpty()) {
            throw new ConflictException("SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT",
                    "Supplier invoice " + id + " has line items and cannot also have a header vehicle — "
                            + "remove the line items first or leave the header vehicle unset");
        }
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        supplierInvoiceMapper.updateEntity(request, invoice);
        invoice.setVehicle(vehicle);
        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        return toResponseWithLineItems(saved, existingLineItems);
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
        // Reconciliation only applies to invoices that use per-vehicle line-item splitting (no
        // header vehicle). An invoice with a header vehicle can never have line items (mutual
        // exclusion enforced in update()/addLineItem()), so it's skipped entirely rather than
        // fetching a list that would always be empty.
        List<SupplierInvoiceLineItem> lineItems = invoice.getVehicle() == null
                ? supplierInvoiceLineItemRepository.findAllByInvoiceId(id)
                : List.of();
        if (!lineItems.isEmpty()) {
            BigDecimal linesSum = lineItems.stream()
                    .map(SupplierInvoiceLineItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            if (linesSum.compareTo(invoice.getSubtotal()) != 0) {
                throw new ConflictException("SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE",
                        "Supplier invoice " + id + " line items total " + linesSum
                                + " but the invoice subtotal is " + invoice.getSubtotal()
                                + " — allocate the remaining " + invoice.getSubtotal().subtract(linesSum)
                                + " before marking as paid");
            }
        }
        invoice.setStatus(SupplierInvoiceStatus.PAID);
        invoice.setPaymentDate(request != null && request.paymentDate() != null
                ? request.paymentDate() : LocalDate.now());
        SupplierInvoice saved = supplierInvoiceRepository.save(invoice);
        // Reuse the lineItems already fetched above - no extra query needed.
        return toResponseWithLineItems(saved, lineItems);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierLineItemResponse addLineItem(UUID invoiceId, SupplierLineItemRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + invoiceId + " cannot receive line items from state " + invoice.getStatus());
        if (invoice.getVehicle() != null) {
            throw new ConflictException("SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT",
                    "Supplier invoice " + invoiceId + " has a header vehicle and cannot also receive line items — "
                            + "clear the header vehicle first");
        }
        Vehicle linkedVehicle = resolveVehicle(request.vehicleId());
        MaintenanceRecord linkedMaintenance = resolveMaintenance(request.maintenanceRecordId());

        SupplierInvoiceLineItem lineItem = supplierInvoiceMapper.toEntity(request);
        lineItem.setInvoice(invoice);
        lineItem.setVehicle(linkedVehicle);
        lineItem.setMaintenanceRecord(linkedMaintenance);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return supplierInvoiceMapper.toResponse(supplierInvoiceLineItemRepository.save(lineItem));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public SupplierLineItemResponse updateLineItem(UUID invoiceId, UUID lineItemId, SupplierLineItemRequest request) {
        SupplierInvoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + invoiceId + " line items cannot be modified from state " + invoice.getStatus());
        SupplierInvoiceLineItem lineItem = findLineItemOrThrow(invoiceId, lineItemId);
        Vehicle linkedVehicle = resolveVehicle(request.vehicleId());
        MaintenanceRecord linkedMaintenance = resolveMaintenance(request.maintenanceRecordId());

        lineItem.setDescription(request.description());
        lineItem.setQuantity(request.quantity());
        lineItem.setUnitPrice(request.unitPrice());
        lineItem.setVehicle(linkedVehicle);
        lineItem.setMaintenanceRecord(linkedMaintenance);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return supplierInvoiceMapper.toResponse(supplierInvoiceLineItemRepository.save(lineItem));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void deleteLineItem(UUID invoiceId, UUID lineItemId) {
        SupplierInvoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsPending(invoice, "SUPPLIER_INVOICE_INVALID_STATE_TRANSITION",
                "Supplier invoice " + invoiceId + " line items cannot be modified from state " + invoice.getStatus());
        SupplierInvoiceLineItem lineItem = findLineItemOrThrow(invoiceId, lineItemId);
        supplierInvoiceLineItemRepository.delete(lineItem);
    }

    private SupplierInvoiceLineItem findLineItemOrThrow(UUID invoiceId, UUID lineItemId) {
        return supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)
                .orElseThrow(() -> new NotFoundException("SUPPLIER_LINE_ITEM_NOT_FOUND",
                        "Line item " + lineItemId + " not found on invoice " + invoiceId));
    }

    // MapStruct can't express "attach a batched/pre-fetched collection" via @Mapping, so the base
    // response is mapped normally (lineItems left unmapped, see SupplierInvoiceMapper) and the
    // line items — resolved by the caller via whichever query strategy fits (single lookup for
    // getById(), batched lookup for list()) — are attached here via the record's canonical
    // constructor. Mirrors InvoiceService.toResponseWithLineItems().
    private SupplierInvoiceResponse toResponseWithLineItems(SupplierInvoice invoice, List<SupplierInvoiceLineItem> lineItems) {
        SupplierInvoiceResponse base = supplierInvoiceMapper.toResponse(invoice);
        List<SupplierLineItemResponse> lineItemResponses = lineItems.stream()
                .map(supplierInvoiceMapper::toResponse)
                .toList();
        return new SupplierInvoiceResponse(base.id(), base.supplierName(), base.supplierInvoiceNumber(),
                base.category(), base.invoiceDate(), base.dueDate(), base.paymentDate(), base.status(),
                base.subtotal(), base.taxAmount(), base.total(), base.vehicleId(), base.vehicleLicensePlate(),
                base.vehicleMake(), base.vehicleModel(), base.notes(), base.documentPath(), base.createdAt(),
                lineItemResponses);
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

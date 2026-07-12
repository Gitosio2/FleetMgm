package com.fleetmgm.billing.application;

import com.fleetmgm.auth.domain.User;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceServiceTest {

    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock SupplierInvoiceMapper supplierInvoiceMapper;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @InjectMocks SupplierInvoiceService supplierInvoiceService;

    private static final UUID SUPPLIER_ID = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsPendingInvoice_withResolvedVehicle() {
        UUID vehicleId = UUID.randomUUID();
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                SUPPLIER_ID, "SUP-001", ExpenseCategory.MAINTENANCE, LocalDate.now(), null,
                vehicleId, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        Supplier supplier = new Supplier();
        Vehicle vehicle = new Vehicle();
        SupplierInvoice entity = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        SupplierInvoiceResponse result = supplierInvoiceService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<SupplierInvoice> captor = ArgumentCaptor.forClass(SupplierInvoice.class);
        verify(supplierInvoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(captor.getValue().getSupplier()).isEqualTo(supplier);
    }

    @Test
    void create_allowsNullVehicle_whenNotProvided() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                SUPPLIER_ID, null, ExpenseCategory.OTHER, LocalDate.now(), null,
                null, new BigDecimal("50.00"), new BigDecimal("0"), new BigDecimal("50.00"), null, null);
        Supplier supplier = new Supplier();
        SupplierInvoice entity = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        supplierInvoiceService.create(request);

        verify(vehicleRepository, never()).findById(any());
        ArgumentCaptor<SupplierInvoice> captor = ArgumentCaptor.forClass(SupplierInvoice.class);
        verify(supplierInvoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isNull();
    }

    @Test
    void create_throwsNotFound_whenVehicleMissing() {
        UUID vehicleId = UUID.randomUUID();
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                SUPPLIER_ID, null, ExpenseCategory.OTHER, LocalDate.now(), null,
                vehicleId, new BigDecimal("50.00"), new BigDecimal("0"), new BigDecimal("50.00"), null, null);

        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(new Supplier()));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

        verify(supplierInvoiceRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenSupplierMissing() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                SUPPLIER_ID, null, ExpenseCategory.OTHER, LocalDate.now(), null,
                null, new BigDecimal("50.00"), new BigDecimal("0"), new BigDecimal("50.00"), null, null);

        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_NOT_FOUND"));

        verify(supplierInvoiceRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_returnsMappedInvoice_whenFound() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        assertThat(supplierInvoiceService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_INVOICE_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_updatesFields_whenPending() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                SUPPLIER_ID, "SUP-002", ExpenseCategory.FUEL, LocalDate.now(), null,
                vehicleId, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        Supplier supplier = new Supplier();
        Vehicle vehicle = new Vehicle();
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        SupplierInvoiceResponse result = supplierInvoiceService.update(id, request);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getVehicle()).isEqualTo(vehicle);
        assertThat(invoice.getSupplier()).isEqualTo(supplier);
        verify(supplierInvoiceMapper).updateEntity(request, invoice);
    }

    @Test
    void update_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                SUPPLIER_ID, null, ExpenseCategory.FUEL, LocalDate.now(), null,
                null, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));

        verify(supplierInvoiceRepository, never()).save(any());
    }

    @Test
    void update_throwsNotFound_whenSupplierMissing() {
        UUID id = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                SUPPLIER_ID, null, ExpenseCategory.FUEL, LocalDate.now(), null,
                null, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.update(id, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_NOT_FOUND"));

        verify(supplierInvoiceRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletesPendingInvoice_andWritesAuditLog() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        setId(invoice, id);
        invoice.setStatus(SupplierInvoiceStatus.PENDING);

        User user = new User();
        setId(user, userId);
        user.setEmail("manager@fleetmgm.com");

        setAuthentication("manager@fleetmgm.com");
        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(userRepository.findByEmail("manager@fleetmgm.com")).thenReturn(Optional.of(user));

        supplierInvoiceService.delete(id);

        assertThat(invoice.getDeletedAt()).isNotNull();
        verify(supplierInvoiceRepository).save(invoice);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(log.getEntityType()).isEqualTo("SupplierInvoice");
        assertThat(log.getEntityId()).isEqualTo(id.toString());
        assertThat(log.getPerformedByEmail()).isEqualTo("manager@fleetmgm.com");
        assertThat(log.getPerformedByUserId()).isEqualTo(userId);
    }

    @Test
    void delete_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.delete(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_DELETE_NOT_ALLOWED"));

        verify(supplierInvoiceRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // --- pay ---

    @Test
    void pay_transitionsToPaid_usingCurrentDate_whenRequestIsNull() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        SupplierInvoiceResponse result = supplierInvoiceService.pay(id, null);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
        assertThat(invoice.getPaymentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void pay_transitionsToPaid_usingCurrentDate_whenRequestPaymentDateIsNull() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(buildResponse(id));

        supplierInvoiceService.pay(id, new PayInvoiceRequest(null));

        assertThat(invoice.getPaymentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void pay_usesProvidedPaymentDate_whenPresentInRequest() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        LocalDate pastDate = LocalDate.now().minusDays(10);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(buildResponse(id));

        supplierInvoiceService.pay(id, new PayInvoiceRequest(pastDate));

        assertThat(invoice.getPaymentDate()).isEqualTo(pastDate);
    }

    @Test
    void pay_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.pay(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));

        verify(supplierInvoiceRepository, never()).save(any());
    }

    // --- addLineItem ---

    @Test
    void addLineItem_persistsLine_whenPending() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", new BigDecimal("2"), new BigDecimal("50.00"), null, null);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        SupplierLineItemResponse result = supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getInvoice()).isEqualTo(invoice);
        assertThat(entity.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void addLineItem_roundsSubtotalToTwoDecimals_whenMultiplicationProducesMore() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        // 3 * 10.005 = 30.015 -> HALF_UP to scale 2 = 30.02, not the raw scale-3 value.
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", new BigDecimal("3"), new BigDecimal("10.005"), null, null);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("3"), new BigDecimal("10.005"), new BigDecimal("30.02"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(entity.getSubtotal()).isEqualByComparingTo("30.02");
        assertThat(entity.getSubtotal().scale()).isEqualTo(2);
    }

    @Test
    void addLineItem_throwsConflict_whenInvoiceNotPending() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_throwsNotFound_whenLinkedVehicleMissing() {
        UUID invoiceId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), vehicleId, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_throwsNotFound_whenLinkedMaintenanceMissing() {
        UUID invoiceId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Maintenance", BigDecimal.ONE, new BigDecimal("50.00"), null, maintenanceId);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MAINTENANCE_NOT_FOUND"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_resolvesLinkedVehicleAndMaintenance_whenProvided() {
        UUID invoiceId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        Vehicle vehicle = new Vehicle();
        MaintenanceRecord maintenance = new MaintenanceRecord();
        SupplierLineItemRequest request = new SupplierLineItemRequest("Job", BigDecimal.ONE, new BigDecimal("50.00"), vehicleId, maintenanceId);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Job",
                BigDecimal.ONE, new BigDecimal("50.00"), new BigDecimal("50.00"), vehicleId, maintenanceId);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(maintenance));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(entity.getVehicle()).isEqualTo(vehicle);
        assertThat(entity.getMaintenanceRecord()).isEqualTo(maintenance);
    }

    @Test
    void addLineItem_doesNotTouchInvoiceTotals() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("21.00"));
        invoice.setTotal(new BigDecimal("121.00"));
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", new BigDecimal("2"), new BigDecimal("50.00"), null, null);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(invoice.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("21.00");
        assertThat(invoice.getTotal()).isEqualByComparingTo("121.00");
        verify(supplierInvoiceRepository, never()).save(invoice);
    }

    // --- list ---

    @Test
    void list_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        SupplierInvoice invoice = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(supplierInvoiceRepository.findAllJoinFetch(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        PageResponse<SupplierInvoiceResponse> result = supplierInvoiceService.list(null, null, pageable);

        assertThat(result.content()).containsExactly(expected);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    private static void setAuthentication(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }

    private SupplierInvoiceResponse buildResponse(UUID id) {
        return new SupplierInvoiceResponse(id, SUPPLIER_ID, "Acme Parts", "SUP-001", ExpenseCategory.MAINTENANCE,
                LocalDate.now(), null, null, SupplierInvoiceStatus.PENDING,
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"),
                null, null, null, null, null, null, Instant.now());
    }
}

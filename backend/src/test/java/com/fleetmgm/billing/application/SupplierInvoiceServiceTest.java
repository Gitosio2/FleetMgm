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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceServiceTest {

    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock SupplierInvoiceMapper supplierInvoiceMapper;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @InjectMocks SupplierInvoiceService supplierInvoiceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsPendingInvoice_withResolvedVehicle() {
        UUID vehicleId = UUID.randomUUID();
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                "Acme Parts", "SUP-001", ExpenseCategory.MAINTENANCE, LocalDate.now(), null,
                vehicleId, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        Vehicle vehicle = new Vehicle();
        SupplierInvoice entity = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        SupplierInvoiceResponse result = supplierInvoiceService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<SupplierInvoice> captor = ArgumentCaptor.forClass(SupplierInvoice.class);
        verify(supplierInvoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
    }

    @Test
    void create_allowsNullVehicle_whenNotProvided() {
        CreateSupplierInvoiceRequest request = new CreateSupplierInvoiceRequest(
                "Acme Parts", null, ExpenseCategory.OTHER, LocalDate.now(), null,
                null, new BigDecimal("50.00"), new BigDecimal("0"), new BigDecimal("50.00"), null, null);
        SupplierInvoice entity = new SupplierInvoice();
        SupplierInvoiceResponse expected = buildResponse(UUID.randomUUID());

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
                "Acme Parts", null, ExpenseCategory.OTHER, LocalDate.now(), null,
                vehicleId, new BigDecimal("50.00"), new BigDecimal("0"), new BigDecimal("50.00"), null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

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

    @Test
    void getById_populatesLineItems_whenInvoiceHasLines() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        SupplierInvoiceLineItem line1 = new SupplierInvoiceLineItem();
        SupplierLineItemResponse response1 = new SupplierLineItemResponse(UUID.randomUUID(), "Fuel - Truck A",
                new BigDecimal("40"), new BigDecimal("1.50"), new BigDecimal("60.00"), UUID.randomUUID(), null);
        SupplierInvoiceResponse mapped = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(mapped);
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(line1));
        when(supplierInvoiceMapper.toResponse(line1)).thenReturn(response1);

        SupplierInvoiceResponse result = supplierInvoiceService.getById(id);

        assertThat(result.lineItems()).containsExactly(response1);
    }

    @Test
    void getById_returnsEmptyLineItems_notNull_whenInvoiceHasNoLines() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        SupplierInvoiceResponse mapped = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(mapped);
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of());

        SupplierInvoiceResponse result = supplierInvoiceService.getById(id);

        assertThat(result.lineItems()).isNotNull().isEmpty();
    }

    // --- update ---

    @Test
    void update_updatesFields_whenPending() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                "Acme Parts", "SUP-002", ExpenseCategory.FUEL, LocalDate.now(), null,
                vehicleId, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        Vehicle vehicle = new Vehicle();
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        SupplierInvoiceResponse result = supplierInvoiceService.update(id, request);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getVehicle()).isEqualTo(vehicle);
        verify(supplierInvoiceMapper).updateEntity(request, invoice);
    }

    @Test
    void update_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                "Acme Parts", null, ExpenseCategory.FUEL, LocalDate.now(), null,
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
    void update_throwsConflict_whenSettingHeaderVehicle_andLineItemsExist() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                "Acme Parts", "SUP-002", ExpenseCategory.FUEL, LocalDate.now(), null,
                vehicleId, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceLineItem existingLine = new SupplierInvoiceLineItem();

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(existingLine));

        assertThatThrownBy(() -> supplierInvoiceService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT"));

        verify(supplierInvoiceRepository, never()).save(any());
        verify(vehicleRepository, never()).findById(any());
    }

    @Test
    void update_allowsClearingHeaderVehicle_whenLineItemsExist() {
        UUID id = UUID.randomUUID();
        UpdateSupplierInvoiceRequest request = new UpdateSupplierInvoiceRequest(
                "Acme Parts", "SUP-002", ExpenseCategory.FUEL, LocalDate.now(), null,
                null, new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"), null, null);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceLineItem existingLine = new SupplierInvoiceLineItem();
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(existingLine));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);
        when(supplierInvoiceMapper.toResponse(existingLine)).thenReturn(new SupplierLineItemResponse(
                UUID.randomUUID(), "Parts", BigDecimal.ONE, new BigDecimal("50.00"), new BigDecimal("50.00"), null, null));

        supplierInvoiceService.update(id, request);

        assertThat(invoice.getVehicle()).isNull();
        verify(supplierInvoiceRepository).save(invoice);
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

    @Test
    void pay_throwsConflict_whenLineItemsDoNotReconcileWithSubtotal() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setSubtotal(new BigDecimal("100.00"));
        SupplierInvoiceLineItem line = new SupplierInvoiceLineItem();
        line.setSubtotal(new BigDecimal("60.00"));

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(line));

        assertThatThrownBy(() -> supplierInvoiceService.pay(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_ALLOCATION_INCOMPLETE"));

        verify(supplierInvoiceRepository, never()).save(any());
        assertThat(invoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PENDING);
    }

    @Test
    void pay_succeeds_whenLineItemsReconcileWithSubtotal() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setSubtotal(new BigDecimal("100.00"));
        SupplierInvoiceLineItem line1 = new SupplierInvoiceLineItem();
        line1.setSubtotal(new BigDecimal("60.00"));
        SupplierInvoiceLineItem line2 = new SupplierInvoiceLineItem();
        line2.setSubtotal(new BigDecimal("40.00"));
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(line1, line2));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);
        when(supplierInvoiceMapper.toResponse(line1)).thenReturn(new SupplierLineItemResponse(
                UUID.randomUUID(), "Fuel A", BigDecimal.ONE, new BigDecimal("60.00"), new BigDecimal("60.00"), null, null));
        when(supplierInvoiceMapper.toResponse(line2)).thenReturn(new SupplierLineItemResponse(
                UUID.randomUUID(), "Fuel B", BigDecimal.ONE, new BigDecimal("40.00"), new BigDecimal("40.00"), null, null));

        SupplierInvoiceResponse result = supplierInvoiceService.pay(id, null);

        assertThat(invoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
        assertThat(result.lineItems()).hasSize(2);
    }

    @Test
    void pay_skipsReconciliationCheck_whenHeaderVehicleSet() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setVehicle(new Vehicle());
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        supplierInvoiceService.pay(id, null);

        assertThat(invoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
        verify(supplierInvoiceLineItemRepository, never()).findAllByInvoiceId(any());
    }

    @Test
    void pay_skipsReconciliationCheck_whenNoLineItems() {
        UUID id = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setSubtotal(new BigDecimal("100.00"));
        SupplierInvoiceResponse expected = buildResponse(id);

        when(supplierInvoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of());
        when(supplierInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(supplierInvoiceMapper.toResponse(invoice)).thenReturn(expected);

        supplierInvoiceService.pay(id, null);

        assertThat(invoice.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
    }

    // --- addLineItem ---

    @Test
    void addLineItem_persistsLine_whenPending() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        // 48.30 (total cost, user-entered) / 30 (quantity) = 1.61 average unit price, derived.
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", new BigDecimal("30"), new BigDecimal("48.30"), null, null);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        entity.setSubtotal(new BigDecimal("48.30")); // simulates the real mapper mapping subtotal directly
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("30"), new BigDecimal("1.61"), new BigDecimal("48.30"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        SupplierLineItemResponse result = supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getInvoice()).isEqualTo(invoice);
        assertThat(entity.getUnitPrice()).isEqualByComparingTo("1.61");
    }

    @Test
    void addLineItem_roundsUnitPriceToTwoDecimals_whenDivisionProducesMore() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        // 26.92 / 8 = 3.365 exactly -> HALF_UP to scale 2 = 3.37 (HALF_EVEN would give 3.36 instead).
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", new BigDecimal("8"), new BigDecimal("26.92"), null, null);
        SupplierInvoiceLineItem entity = new SupplierInvoiceLineItem();
        entity.setSubtotal(new BigDecimal("26.92"));
        SupplierLineItemResponse expected = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("8"), new BigDecimal("3.37"), new BigDecimal("26.92"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceMapper.toEntity(request)).thenReturn(entity);
        when(supplierInvoiceLineItemRepository.save(entity)).thenReturn(entity);
        when(supplierInvoiceMapper.toResponse(entity)).thenReturn(expected);

        supplierInvoiceService.addLineItem(invoiceId, request);

        assertThat(entity.getUnitPrice()).isEqualByComparingTo("3.37");
        assertThat(entity.getUnitPrice().scale()).isEqualTo(2);
    }

    @Test
    void addLineItem_throwsConflict_whenHeaderVehicleAlreadySet() {
        UUID invoiceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        invoice.setVehicle(new Vehicle());
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_VEHICLE_LINE_ITEMS_CONFLICT"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
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

    // --- updateLineItem ---

    @Test
    void updateLineItem_updatesFieldsAndRecomputesUnitPrice_whenPending() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();
        SupplierLineItemRequest request = new SupplierLineItemRequest("Updated Parts", new BigDecimal("3"), new BigDecimal("60.00"), null, null);
        SupplierLineItemResponse expected = new SupplierLineItemResponse(lineItemId, "Updated Parts",
                new BigDecimal("3"), new BigDecimal("20.00"), new BigDecimal("60.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)).thenReturn(Optional.of(lineItem));
        when(supplierInvoiceLineItemRepository.save(lineItem)).thenReturn(lineItem);
        when(supplierInvoiceMapper.toResponse(lineItem)).thenReturn(expected);

        SupplierLineItemResponse result = supplierInvoiceService.updateLineItem(invoiceId, lineItemId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(lineItem.getDescription()).isEqualTo("Updated Parts");
        assertThat(lineItem.getQuantity()).isEqualByComparingTo("3");
        assertThat(lineItem.getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(lineItem.getUnitPrice()).isEqualByComparingTo("20.00");
    }

    @Test
    void updateLineItem_resolvesLinkedVehicleAndMaintenance_whenProvided() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();
        Vehicle vehicle = new Vehicle();
        MaintenanceRecord maintenance = new MaintenanceRecord();
        SupplierLineItemRequest request = new SupplierLineItemRequest("Job", BigDecimal.ONE, new BigDecimal("50.00"), vehicleId, maintenanceId);
        SupplierLineItemResponse expected = new SupplierLineItemResponse(lineItemId, "Job",
                BigDecimal.ONE, new BigDecimal("50.00"), new BigDecimal("50.00"), vehicleId, maintenanceId);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)).thenReturn(Optional.of(lineItem));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(maintenance));
        when(supplierInvoiceLineItemRepository.save(lineItem)).thenReturn(lineItem);
        when(supplierInvoiceMapper.toResponse(lineItem)).thenReturn(expected);

        supplierInvoiceService.updateLineItem(invoiceId, lineItemId, request);

        assertThat(lineItem.getVehicle()).isEqualTo(vehicle);
        assertThat(lineItem.getMaintenanceRecord()).isEqualTo(maintenance);
    }

    @Test
    void updateLineItem_throwsConflict_whenInvoiceNotPending() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.updateLineItem(invoiceId, lineItemId, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
    }

    @Test
    void updateLineItem_throwsNotFound_whenLineItemDoesNotBelongToInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierLineItemRequest request = new SupplierLineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), null, null);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.updateLineItem(invoiceId, lineItemId, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_LINE_ITEM_NOT_FOUND"));

        verify(supplierInvoiceLineItemRepository, never()).save(any());
    }

    // --- deleteLineItem ---

    @Test
    void deleteLineItem_removesLineItem_whenPending() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)).thenReturn(Optional.of(lineItem));

        supplierInvoiceService.deleteLineItem(invoiceId, lineItemId);

        verify(supplierInvoiceLineItemRepository).delete(lineItem);
    }

    @Test
    void deleteLineItem_throwsConflict_whenInvoiceNotPending() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PAID);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> supplierInvoiceService.deleteLineItem(invoiceId, lineItemId))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));

        verify(supplierInvoiceLineItemRepository, never()).delete(any());
    }

    @Test
    void deleteLineItem_throwsNotFound_whenLineItemDoesNotBelongToInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID lineItemId = UUID.randomUUID();
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setStatus(SupplierInvoiceStatus.PENDING);

        when(supplierInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(supplierInvoiceLineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierInvoiceService.deleteLineItem(invoiceId, lineItemId))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_LINE_ITEM_NOT_FOUND"));

        verify(supplierInvoiceLineItemRepository, never()).delete(any());
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

    @Test
    void list_populatesLineItems_batchedAcrossPage_notPerInvoice() {
        Pageable pageable = PageRequest.of(0, 20);
        UUID invoiceId1 = UUID.randomUUID();
        UUID invoiceId2 = UUID.randomUUID();
        SupplierInvoice invoice1 = new SupplierInvoice();
        setId(invoice1, invoiceId1);
        SupplierInvoice invoice2 = new SupplierInvoice();
        setId(invoice2, invoiceId2);

        SupplierInvoiceLineItem line1 = new SupplierInvoiceLineItem();
        line1.setInvoice(invoice1);
        SupplierInvoiceLineItem line2 = new SupplierInvoiceLineItem();
        line2.setInvoice(invoice2);
        SupplierInvoiceLineItem line3 = new SupplierInvoiceLineItem();
        line3.setInvoice(invoice1);

        SupplierLineItemResponse response1 = new SupplierLineItemResponse(UUID.randomUUID(), "Line1",
                BigDecimal.ONE, new BigDecimal("10.00"), new BigDecimal("10.00"), null, null);
        SupplierLineItemResponse response2 = new SupplierLineItemResponse(UUID.randomUUID(), "Line2",
                BigDecimal.ONE, new BigDecimal("20.00"), new BigDecimal("20.00"), null, null);
        SupplierLineItemResponse response3 = new SupplierLineItemResponse(UUID.randomUUID(), "Line3",
                BigDecimal.ONE, new BigDecimal("30.00"), new BigDecimal("30.00"), null, null);

        SupplierInvoiceResponse mapped1 = buildResponse(invoiceId1);
        SupplierInvoiceResponse mapped2 = buildResponse(invoiceId2);

        when(supplierInvoiceRepository.findAllJoinFetch(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(invoice1, invoice2), pageable, 2));
        when(supplierInvoiceMapper.toResponse(invoice1)).thenReturn(mapped1);
        when(supplierInvoiceMapper.toResponse(invoice2)).thenReturn(mapped2);
        when(supplierInvoiceLineItemRepository.findAllByInvoiceIdIn(List.of(invoiceId1, invoiceId2)))
                .thenReturn(List.of(line1, line2, line3));
        when(supplierInvoiceMapper.toResponse(line1)).thenReturn(response1);
        when(supplierInvoiceMapper.toResponse(line2)).thenReturn(response2);
        when(supplierInvoiceMapper.toResponse(line3)).thenReturn(response3);

        PageResponse<SupplierInvoiceResponse> result = supplierInvoiceService.list(null, null, pageable);

        SupplierInvoiceResponse resultInvoice1 = result.content().stream()
                .filter(r -> r.id().equals(invoiceId1)).findFirst().orElseThrow();
        SupplierInvoiceResponse resultInvoice2 = result.content().stream()
                .filter(r -> r.id().equals(invoiceId2)).findFirst().orElseThrow();

        assertThat(resultInvoice1.lineItems()).containsExactly(response1, response3);
        assertThat(resultInvoice2.lineItems()).containsExactly(response2);
        verify(supplierInvoiceLineItemRepository, times(1)).findAllByInvoiceIdIn(anyList());
        verify(supplierInvoiceLineItemRepository, never()).findAllByInvoiceId(any());
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
        return new SupplierInvoiceResponse(id, "Acme Parts", "SUP-001", ExpenseCategory.MAINTENANCE,
                LocalDate.now(), null, null, SupplierInvoiceStatus.PENDING,
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"),
                null, null, null, null, null, null, Instant.now(), List.of());
    }
}

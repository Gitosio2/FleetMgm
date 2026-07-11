package com.fleetmgm.billing.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.dto.CreateInvoiceRequest;
import com.fleetmgm.billing.dto.InvoiceMapper;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemRequest;
import com.fleetmgm.billing.dto.LineItemResponse;
import com.fleetmgm.billing.dto.UpdateInvoiceRequest;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
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
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock LineItemRepository lineItemRepository;
    @Mock ClientRepository clientRepository;
    @Mock JobRepository jobRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock InvoiceMapper invoiceMapper;
    @Mock InvoiceNumberGenerator invoiceNumberGenerator;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    InvoiceService invoiceService;

    // Configured default tax rate mirrors billing.default-tax-rate in application.yml (21%).
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.2100");

    @BeforeEach
    void setUp() {
        // @InjectMocks can't be used here: BigDecimal has no @Mock, so Mockito's constructor-injection
        // engine would silently pass null for defaultTaxRate instead of the configured value. Constructed
        // manually once, matching the pattern already used in MaintenanceServiceTest for its @Value param.
        invoiceService = new InvoiceService(invoiceRepository, lineItemRepository, clientRepository,
                jobRepository, maintenanceRepository, invoiceMapper, invoiceNumberGenerator,
                auditLogRepository, userRepository, DEFAULT_TAX_RATE);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsDraftInvoice_withGeneratedNumber() {
        UUID clientId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest(clientId, null, null, null);
        Client client = new Client();
        Invoice entity = new Invoice();
        InvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(invoiceNumberGenerator.generate()).thenReturn("INV-2026-00001");
        when(invoiceRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(expected);

        InvoiceResponse result = invoiceService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isEqualTo(client);
        assertThat(captor.getValue().getInvoiceNumber()).isEqualTo("INV-2026-00001");
    }

    @Test
    void create_throwsNotFound_whenClientMissing() {
        UUID clientId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest(clientId, null, null, null);

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("CLIENT_NOT_FOUND"));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void create_usesConfiguredDefaultTaxRate_whenNotProvidedInRequest() {
        UUID clientId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest(clientId, null, null, null);
        Client client = new Client();
        Invoice entity = new Invoice();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(invoiceNumberGenerator.generate()).thenReturn("INV-2026-00001");
        when(invoiceRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(buildResponse(UUID.randomUUID()));

        invoiceService.create(request);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getTaxRate()).isEqualByComparingTo(DEFAULT_TAX_RATE);
    }

    @Test
    void create_usesProvidedTaxRate_whenPresentInRequest() {
        UUID clientId = UUID.randomUUID();
        BigDecimal reducedRate = new BigDecimal("0.10");
        CreateInvoiceRequest request = new CreateInvoiceRequest(clientId, null, null, reducedRate);
        Client client = new Client();
        Invoice entity = new Invoice();

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(invoiceNumberGenerator.generate()).thenReturn("INV-2026-00001");
        when(invoiceRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(buildResponse(UUID.randomUUID()));

        invoiceService.create(request);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getTaxRate()).isEqualByComparingTo(reducedRate);
    }

    // --- getById ---

    @Test
    void getById_returnsMappedInvoice_whenFound() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        InvoiceResponse expected = buildResponse(id);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toResponse(invoice)).thenReturn(expected);

        assertThat(invoiceService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("INVOICE_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_updatesFields_whenDraft() {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UpdateInvoiceRequest request = new UpdateInvoiceRequest(clientId, null, "updated notes", null);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        Client client = new Client();
        InvoiceResponse expected = buildResponse(id);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(expected);

        InvoiceResponse result = invoiceService.update(id, request);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getClient()).isEqualTo(client);
        verify(invoiceMapper).updateEntity(request, invoice);
    }

    @Test
    void update_throwsConflict_whenNotDraft() {
        UUID id = UUID.randomUUID();
        UpdateInvoiceRequest request = new UpdateInvoiceRequest(UUID.randomUUID(), null, null, null);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.ISSUED);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("INVOICE_INVALID_STATE_TRANSITION"));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void update_overridesTaxRate_whenProvided() {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal newRate = new BigDecimal("0.10");
        UpdateInvoiceRequest request = new UpdateInvoiceRequest(clientId, null, null, newRate);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        Client client = new Client();

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(buildResponse(id));

        invoiceService.update(id, request);

        assertThat(invoice.getTaxRate()).isEqualByComparingTo(newRate);
    }

    @Test
    void update_leavesExistingTaxRate_unchanged_whenTaxRateNull() {
        UUID id = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        BigDecimal existingRate = new BigDecimal("0.10");
        UpdateInvoiceRequest request = new UpdateInvoiceRequest(clientId, null, null, null);
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTaxRate(existingRate);
        Client client = new Client();

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(buildResponse(id));

        invoiceService.update(id, request);

        assertThat(invoice.getTaxRate()).isEqualByComparingTo(existingRate);
    }

    // --- delete ---

    @Test
    void delete_softDeletesDraftInvoice_andWritesAuditLog() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        setId(invoice, id);
        invoice.setStatus(InvoiceStatus.DRAFT);

        User user = new User();
        setId(user, userId);
        user.setEmail("manager@fleetmgm.com");

        setAuthentication("manager@fleetmgm.com");
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(userRepository.findByEmail("manager@fleetmgm.com")).thenReturn(Optional.of(user));

        invoiceService.delete(id);

        assertThat(invoice.getDeletedAt()).isNotNull();
        verify(invoiceRepository).save(invoice);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(log.getEntityType()).isEqualTo("Invoice");
        assertThat(log.getEntityId()).isEqualTo(id.toString());
        assertThat(log.getPerformedByEmail()).isEqualTo("manager@fleetmgm.com");
        assertThat(log.getPerformedByUserId()).isEqualTo(userId);
    }

    @Test
    void delete_throwsConflict_whenNotDraft() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.ISSUED);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.delete(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("INVOICE_DELETE_NOT_ALLOWED"));

        verify(invoiceRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    // --- addLineItem ---

    @Test
    void addLineItem_persistsLine_whenDraft() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        LineItemRequest request = new LineItemRequest("Parts", new BigDecimal("2"), new BigDecimal("50.00"), null, null);
        InvoiceLineItem entity = new InvoiceLineItem();
        LineItemResponse expected = new LineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(lineItemRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(expected);

        LineItemResponse result = invoiceService.addLineItem(invoiceId, request);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getInvoice()).isEqualTo(invoice);
        assertThat(entity.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void addLineItem_roundsSubtotalToTwoDecimals_whenMultiplicationProducesMore() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        // 3 * 10.005 = 30.015 -> HALF_UP to scale 2 = 30.02, not the raw scale-3 value.
        LineItemRequest request = new LineItemRequest("Parts", new BigDecimal("3"), new BigDecimal("10.005"), null, null);
        InvoiceLineItem entity = new InvoiceLineItem();
        LineItemResponse expected = new LineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("3"), new BigDecimal("10.005"), new BigDecimal("30.02"), null, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(lineItemRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(expected);

        invoiceService.addLineItem(invoiceId, request);

        assertThat(entity.getSubtotal()).isEqualByComparingTo("30.02");
        assertThat(entity.getSubtotal().scale()).isEqualTo(2);
    }

    @Test
    void addLineItem_throwsConflict_whenInvoiceNotDraft() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.ISSUED);
        LineItemRequest request = new LineItemRequest("Parts", BigDecimal.ONE, new BigDecimal("50.00"), null, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("INVOICE_INVALID_STATE_TRANSITION"));

        verify(lineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_throwsNotFound_whenLinkedJobMissing() {
        UUID invoiceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        LineItemRequest request = new LineItemRequest("Job", BigDecimal.ONE, new BigDecimal("50.00"), jobId, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("JOB_NOT_FOUND"));

        verify(lineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_throwsNotFound_whenLinkedMaintenanceMissing() {
        UUID invoiceId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        LineItemRequest request = new LineItemRequest("Maintenance", BigDecimal.ONE, new BigDecimal("50.00"), null, maintenanceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.addLineItem(invoiceId, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MAINTENANCE_NOT_FOUND"));

        verify(lineItemRepository, never()).save(any());
    }

    @Test
    void addLineItem_resolvesLinkedJob_whenProvided() {
        UUID invoiceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        Job job = new Job();
        LineItemRequest request = new LineItemRequest("Job", BigDecimal.ONE, new BigDecimal("50.00"), jobId, null);
        InvoiceLineItem entity = new InvoiceLineItem();
        LineItemResponse expected = new LineItemResponse(UUID.randomUUID(), "Job",
                BigDecimal.ONE, new BigDecimal("50.00"), new BigDecimal("50.00"), jobId, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(invoiceMapper.toEntity(request)).thenReturn(entity);
        when(lineItemRepository.save(entity)).thenReturn(entity);
        when(invoiceMapper.toResponse(entity)).thenReturn(expected);

        invoiceService.addLineItem(invoiceId, request);

        assertThat(entity.getLinkedJob()).isEqualTo(job);
    }

    // --- issue ---

    @Test
    void issue_computesSubtotalTaxAndTotal_whenAtLeastOneLineItem() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        // taxRate defaults to 0.2100 on the entity

        InvoiceLineItem line1 = new InvoiceLineItem();
        line1.setSubtotal(new BigDecimal("100.00"));
        InvoiceLineItem line2 = new InvoiceLineItem();
        line2.setSubtotal(new BigDecimal("50.00"));

        InvoiceResponse expected = buildResponse(id);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(line1, line2));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(expected);

        InvoiceResponse result = invoiceService.issue(id);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getIssueDate()).isEqualTo(java.time.LocalDate.now());
        // subtotal = 150.00; tax = 150.00 * 0.21 = 31.50; total = 181.50
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("31.50");
        assertThat(invoice.getTotal()).isEqualByComparingTo("181.50");
    }

    @Test
    void issue_computesTaxCorrectly_withNonDefaultTaxRate() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTaxRate(new BigDecimal("0.10"));

        InvoiceLineItem line1 = new InvoiceLineItem();
        line1.setSubtotal(new BigDecimal("100.00"));

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of(line1));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(buildResponse(id));

        invoiceService.issue(id);

        // subtotal = 100.00; tax = 100.00 * 0.10 = 10.00 (would be 21.00 if hardcoded to 21%); total = 110.00
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("10.00");
        assertThat(invoice.getTotal()).isEqualByComparingTo("110.00");
    }

    @Test
    void issue_throwsConflict_whenNoLineItems() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(id)).thenReturn(List.of());

        assertThatThrownBy(() -> invoiceService.issue(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVOICE_NO_LINE_ITEMS"));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void issue_throwsConflict_whenNotDraft() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.ISSUED);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.issue(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("INVOICE_INVALID_STATE_TRANSITION"));

        verify(lineItemRepository, never()).findAllByInvoiceId(any());
        verify(invoiceRepository, never()).save(any());
    }

    // --- pay ---

    @Test
    void pay_transitionsToPaid_whenIssued() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.ISSUED);
        InvoiceResponse expected = buildResponse(id);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(expected);

        InvoiceResponse result = invoiceService.pay(id);

        assertThat(result).isEqualTo(expected);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getPaymentDate()).isEqualTo(java.time.LocalDate.now());
    }

    @Test
    void pay_throwsConflict_whenNotIssued() {
        UUID id = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.DRAFT);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.pay(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("INVOICE_INVALID_STATE_TRANSITION"));

        verify(invoiceRepository, never()).save(any());
    }

    // --- list ---

    @Test
    void list_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Invoice invoice = new Invoice();
        InvoiceResponse expected = buildResponse(UUID.randomUUID());

        when(invoiceRepository.findAllJoinFetch(pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));
        when(invoiceMapper.toResponse(invoice)).thenReturn(expected);

        PageResponse<InvoiceResponse> result = invoiceService.list(pageable);

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

    private InvoiceResponse buildResponse(UUID id) {
        return new InvoiceResponse(id, "INV-2026-00001", UUID.randomUUID(), "Client", InvoiceStatus.DRAFT,
                null, null, null, new BigDecimal("0.2100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, Instant.now());
    }
}

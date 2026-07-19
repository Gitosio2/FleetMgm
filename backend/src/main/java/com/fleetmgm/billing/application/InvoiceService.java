package com.fleetmgm.billing.application;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.dto.CreateInvoiceRequest;
import com.fleetmgm.billing.dto.InvoiceMapper;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemRequest;
import com.fleetmgm.billing.dto.LineItemResponse;
import com.fleetmgm.billing.dto.PayInvoiceRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class InvoiceService {

    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    // Standard 2-decimal currency rounding — no other BigDecimal money math exists elsewhere in
    // this codebase to mirror (MaintenanceRecord.cost is stored, not computed), so HALF_UP at
    // scale 2 is the conventional default for tax/total calculations.
    private static final int MONEY_SCALE = 2;

    private final InvoiceRepository invoiceRepository;
    private final LineItemRepository lineItemRepository;
    private final ClientRepository clientRepository;
    private final JobRepository jobRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BigDecimal defaultTaxRate;

    public InvoiceService(InvoiceRepository invoiceRepository,
                           LineItemRepository lineItemRepository,
                           ClientRepository clientRepository,
                           JobRepository jobRepository,
                           InvoiceMapper invoiceMapper,
                           InvoiceNumberGenerator invoiceNumberGenerator,
                           AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           @Value("${billing.default-tax-rate}") BigDecimal defaultTaxRate) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.clientRepository = clientRepository;
        this.jobRepository = jobRepository;
        this.invoiceMapper = invoiceMapper;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.defaultTaxRate = defaultTaxRate;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<InvoiceResponse> list(UUID clientId, String invoiceNumber, InvoiceStatus status,
            LocalDate issueDateFrom, LocalDate issueDateTo, LocalDate dueDateFrom, LocalDate dueDateTo,
            LocalDate paymentDateFrom, LocalDate paymentDateTo,
            BigDecimal totalMin, BigDecimal totalMax, Pageable pageable) {
        // The repository query's own ORDER BY (pending-first, then newest-first) is the whole
        // point — a caller-supplied Sort would just get appended after it, which is confusing at
        // best, so it's stripped here rather than trusted from the controller.
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Invoice> page = invoiceRepository.findAllJoinFetch(clientId, invoiceNumber, status, issueDateFrom,
                issueDateTo, dueDateFrom, dueDateTo, paymentDateFrom, paymentDateTo, totalMin, totalMax, pageOnly);
        List<UUID> invoiceIds = page.getContent().stream().map(Invoice::getId).toList();
        // Single batched query for the whole page — grouping in memory here, instead of calling
        // lineItemRepository.findAllByInvoiceId() once per invoice inside the loop below, is what
        // keeps this at 2 queries total instead of 1+N (CLAUDE.md JPA N+1 rule).
        Map<UUID, List<InvoiceLineItem>> lineItemsByInvoiceId = lineItemRepository.findAllByInvoiceIdIn(invoiceIds)
                .stream()
                .collect(Collectors.groupingBy(lineItem -> lineItem.getInvoice().getId()));
        return PageResponse.from(page.map(invoice ->
                toResponseWithLineItems(invoice, lineItemsByInvoiceId.getOrDefault(invoice.getId(), List.of()))));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Client client = resolveClient(request.clientId());
        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumberGenerator.generate());
        invoice.setTaxRate(request.taxRate() != null ? request.taxRate() : defaultTaxRate);
        // A brand-new invoice can't have pre-existing line items (they're only added afterward via
        // addLineItem()), so there's nothing to fetch here - skip the query entirely.
        return toResponseWithLineItems(invoiceRepository.save(invoice), List.of());
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public InvoiceResponse getById(UUID id) {
        Invoice invoice = findInvoiceOrThrow(id);
        List<InvoiceLineItem> lineItems = lineItemRepository.findAllByInvoiceId(id);
        return toResponseWithLineItems(invoice, lineItems);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse update(UUID id, UpdateInvoiceRequest request) {
        Invoice invoice = findInvoiceOrThrow(id);
        assertIsDraft(invoice, "INVOICE_INVALID_STATE_TRANSITION",
                "Invoice " + id + " cannot be updated from state " + invoice.getStatus());
        Client client = resolveClient(request.clientId());
        invoiceMapper.updateEntity(request, invoice);
        invoice.setClient(client);
        if (request.taxRate() != null) {
            invoice.setTaxRate(request.taxRate());
        }
        Invoice saved = invoiceRepository.save(invoice);
        return toResponseWithLineItems(saved, lineItemRepository.findAllByInvoiceId(saved.getId()));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        Invoice invoice = findInvoiceOrThrow(id);
        // A DRAFT invoice never had a fiscal number assigned, so it can be truly removed. An
        // ISSUED/OVERDUE invoice already consumed a sequential invoice_number_seq value — deleting
        // it outright would leave a permanent gap in that sequence, which real invoicing/antifraud
        // rules (and Verifactu) treat as suspicious. It's cancelled instead: status flips to
        // CANCELLED but deletedAt stays null, so it remains visible in lists/audit with its number
        // intact, and the user issues a fresh invoice separately if needed.
        switch (invoice.getStatus()) {
            case DRAFT -> invoice.setDeletedAt(Instant.now());
            case ISSUED, OVERDUE -> invoice.setStatus(InvoiceStatus.CANCELLED);
            case PAID, CANCELLED -> throw new ConflictException("INVOICE_DELETE_NOT_ALLOWED",
                    "Invoice " + id + " cannot be deleted from state " + invoice.getStatus());
        }
        invoiceRepository.save(invoice);
        auditDeletion(invoice);
    }

    private void auditDeletion(Invoice invoice) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog log = new AuditLog();
        log.setEntityType("Invoice");
        log.setEntityId(invoice.getId().toString());
        log.setAction(AuditAction.DELETE);
        log.setPerformedByEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> log.setPerformedByUserId(user.getId()));
        log.setPerformedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse issue(UUID id) {
        Invoice invoice = findInvoiceOrThrow(id);
        assertIsDraft(invoice, "INVOICE_INVALID_STATE_TRANSITION",
                "Invoice " + id + " cannot be issued from state " + invoice.getStatus());
        List<InvoiceLineItem> lineItems = lineItemRepository.findAllByInvoiceId(id);
        if (lineItems.isEmpty()) {
            throw new ConflictException("INVOICE_NO_LINE_ITEMS",
                    "Invoice " + id + " cannot be issued without at least one line item");
        }
        BigDecimal subtotal = lineItems.stream()
                .map(InvoiceLineItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(invoice.getTaxRate())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotal(total);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(LocalDate.now());
        // Reuse the lineItems already fetched above for the tax computation - no extra query needed.
        return toResponseWithLineItems(invoiceRepository.save(invoice), lineItems);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse pay(UUID id, PayInvoiceRequest request) {
        Invoice invoice = findInvoiceOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new ConflictException("INVOICE_INVALID_STATE_TRANSITION",
                    "Invoice " + id + " cannot be paid from state " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(request != null && request.paymentDate() != null
                ? request.paymentDate() : LocalDate.now());
        Invoice saved = invoiceRepository.save(invoice);
        return toResponseWithLineItems(saved, lineItemRepository.findAllByInvoiceId(saved.getId()));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public LineItemResponse addLineItem(UUID invoiceId, LineItemRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsDraft(invoice, "INVOICE_INVALID_STATE_TRANSITION",
                "Invoice " + invoiceId + " cannot receive line items from state " + invoice.getStatus());
        Job linkedJob = resolveLinkedJob(request.linkedJobId());

        InvoiceLineItem lineItem = invoiceMapper.toEntity(request);
        lineItem.setInvoice(invoice);
        lineItem.setLinkedJob(linkedJob);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return invoiceMapper.toResponse(lineItemRepository.save(lineItem));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public LineItemResponse updateLineItem(UUID invoiceId, UUID lineItemId, LineItemRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsDraft(invoice, "INVOICE_INVALID_STATE_TRANSITION",
                "Invoice " + invoiceId + " cannot have line items updated from state " + invoice.getStatus());
        InvoiceLineItem lineItem = lineItemRepository.findByIdAndInvoiceId(lineItemId, invoiceId)
                .orElseThrow(() -> new NotFoundException("LINE_ITEM_NOT_FOUND",
                        "Line item " + lineItemId + " not found on invoice " + invoiceId));
        Job linkedJob = resolveLinkedJob(request.linkedJobId());

        invoiceMapper.updateEntity(request, lineItem);
        lineItem.setLinkedJob(linkedJob);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return invoiceMapper.toResponse(lineItemRepository.save(lineItem));
    }

    // --- relation resolution helpers ---

    // MapStruct can't express "attach a batched/pre-fetched collection" via @Mapping, so the base
    // response is mapped normally (lineItems left unmapped, see InvoiceMapper) and the line items —
    // resolved by the caller via whichever query strategy fits (single lookup for getById(),
    // batched lookup for list()) — are attached here via the record's canonical constructor.
    private InvoiceResponse toResponseWithLineItems(Invoice invoice, List<InvoiceLineItem> lineItems) {
        InvoiceResponse base = invoiceMapper.toResponse(invoice);
        List<LineItemResponse> lineItemResponses = lineItems.stream()
                .map(invoiceMapper::toResponse)
                .toList();
        return new InvoiceResponse(base.id(), base.invoiceNumber(), base.clientId(), base.clientName(),
                base.status(), base.issueDate(), base.dueDate(), base.paymentDate(), base.taxRate(),
                base.subtotal(), base.taxAmount(), base.total(), base.notes(), base.createdAt(),
                lineItemResponses);
    }

    private Invoice findInvoiceOrThrow(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("INVOICE_NOT_FOUND", "Invoice " + id + " not found"));
    }

    private void assertIsDraft(Invoice invoice, String code, String message) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException(code, message);
        }
    }

    private Client resolveClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + clientId + " not found"));
    }

    private Job resolveLinkedJob(UUID jobId) {
        if (jobId == null) {
            return null;
        }
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + jobId + " not found"));
    }
}

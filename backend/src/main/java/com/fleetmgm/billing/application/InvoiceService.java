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
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

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
    private final MaintenanceRepository maintenanceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BigDecimal defaultTaxRate;

    public InvoiceService(InvoiceRepository invoiceRepository,
                           LineItemRepository lineItemRepository,
                           ClientRepository clientRepository,
                           JobRepository jobRepository,
                           MaintenanceRepository maintenanceRepository,
                           InvoiceMapper invoiceMapper,
                           InvoiceNumberGenerator invoiceNumberGenerator,
                           AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           @Value("${billing.default-tax-rate}") BigDecimal defaultTaxRate) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.clientRepository = clientRepository;
        this.jobRepository = jobRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.invoiceMapper = invoiceMapper;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.defaultTaxRate = defaultTaxRate;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<InvoiceResponse> list(Pageable pageable) {
        return PageResponse.from(invoiceRepository.findAllJoinFetch(pageable).map(invoiceMapper::toResponse));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Client client = resolveClient(request.clientId());
        Invoice invoice = invoiceMapper.toEntity(request);
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumberGenerator.generate());
        invoice.setTaxRate(request.taxRate() != null ? request.taxRate() : defaultTaxRate);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public InvoiceResponse getById(UUID id) {
        return invoiceMapper.toResponse(findInvoiceOrThrow(id));
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
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        Invoice invoice = findInvoiceOrThrow(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVOICE_DELETE_NOT_ALLOWED",
                    "Invoice " + id + " cannot be deleted from state " + invoice.getStatus());
        }
        invoice.setDeletedAt(Instant.now());
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
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
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
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public LineItemResponse addLineItem(UUID invoiceId, LineItemRequest request) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        assertIsDraft(invoice, "INVOICE_INVALID_STATE_TRANSITION",
                "Invoice " + invoiceId + " cannot receive line items from state " + invoice.getStatus());
        Job linkedJob = resolveLinkedJob(request.linkedJobId());
        MaintenanceRecord linkedMaintenance = resolveLinkedMaintenance(request.linkedMaintenanceId());

        InvoiceLineItem lineItem = invoiceMapper.toEntity(request);
        lineItem.setInvoice(invoice);
        lineItem.setLinkedJob(linkedJob);
        lineItem.setLinkedMaintenance(linkedMaintenance);
        lineItem.setSubtotal(request.quantity().multiply(request.unitPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return invoiceMapper.toResponse(lineItemRepository.save(lineItem));
    }

    // --- relation resolution helpers ---

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

    private MaintenanceRecord resolveLinkedMaintenance(UUID maintenanceId) {
        if (maintenanceId == null) {
            return null;
        }
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + maintenanceId + " not found"));
    }
}

package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.UUID;

// Lives in billing.application, not job.application: it mutates Invoice/InvoiceLineItem, both of
// which belong to this feature — same "listener lives in the package of the entity it mutates"
// rule established for the workshop listeners (Hito 21/24).
@Component
public class InvoiceJobCompletionListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceJobCompletionListener.class);

    private final InvoiceRepository invoiceRepository;
    private final LineItemRepository lineItemRepository;
    private final ClientRepository clientRepository;
    private final JobRepository jobRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    public InvoiceJobCompletionListener(InvoiceRepository invoiceRepository,
                                         LineItemRepository lineItemRepository,
                                         ClientRepository clientRepository,
                                         JobRepository jobRepository,
                                         InvoiceNumberGenerator invoiceNumberGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.clientRepository = clientRepository;
        this.jobRepository = jobRepository;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onJobCompleted(JobCompletedEvent event) {
        // AFTER_COMMIT: the triggering transaction already committed and the original HTTP call
        // already returned 200 OK, so an exception here can't roll anything back — it must be
        // logged with enough context to debug later, never left silent and never rethrown.
        try {
            // A Job with no clientId bills nothing, and a Job with a clientId but no price is an
            // accepted data gap — both are no-ops here, not errors (see Job.price javadoc/CLAUDE.md).
            if (event.clientId() == null || event.price() == null) {
                return;
            }
            Invoice invoice = invoiceRepository
                    .findFirstByClientIdAndStatusOrderByCreatedAtAsc(event.clientId(), InvoiceStatus.DRAFT)
                    .orElseGet(() -> createDraftInvoice(event.clientId()));

            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(event.title() != null ? event.title() : "Job " + event.jobId());
            lineItem.setQuantity(BigDecimal.ONE);
            lineItem.setUnitPrice(event.price());
            lineItem.setSubtotal(event.price());
            // The job that just completed obviously exists, but resolve it defensively the same
            // way every other optional relation in this codebase is resolved — never assume.
            jobRepository.findById(event.jobId()).ifPresent(lineItem::setLinkedJob);
            lineItemRepository.save(lineItem);
        } catch (RuntimeException e) {
            log.error("Failed to add auto-generated invoice line item for completed job {} (client {})",
                    event.jobId(), event.clientId(), e);
        }
    }

    private Invoice createDraftInvoice(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + clientId + " not found"));
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumberGenerator.generate());
        return invoiceRepository.save(invoice);
    }
}

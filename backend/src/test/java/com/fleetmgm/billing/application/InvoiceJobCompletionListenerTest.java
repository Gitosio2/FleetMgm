package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.infrastructure.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceJobCompletionListenerTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock LineItemRepository lineItemRepository;
    @Mock ClientRepository clientRepository;
    @Mock JobRepository jobRepository;
    @Mock InvoiceNumberGenerator invoiceNumberGenerator;
    @InjectMocks InvoiceJobCompletionListener listener;

    @Test
    void onJobCompleted_isNoOp_whenClientIdIsNull() {
        JobCompletedEvent event = new JobCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, new BigDecimal("100.00"), "Delivery",
                5000L, Instant.now());

        listener.onJobCompleted(event);

        verifyNoInteractions(invoiceRepository, lineItemRepository, clientRepository, jobRepository);
    }

    @Test
    void onJobCompleted_addsZeroPriceLine_whenPriceIsNull() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        JobCompletedEvent event = new JobCompletedEvent(
                jobId, UUID.randomUUID(), clientId, null, "Delivery",
                5000L, Instant.now());

        Invoice existingDraft = new Invoice();
        Job job = new Job();

        when(invoiceRepository.findFirstByClientIdAndStatusOrderByCreatedAtAsc(clientId, InvoiceStatus.DRAFT))
                .thenReturn(Optional.of(existingDraft));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        listener.onJobCompleted(event);

        // A missing price must not drop the billable work — it lands as a 0.00 line the billing
        // team can price manually, since unit_price is NOT NULL (InvoiceLineItem).
        ArgumentCaptor<InvoiceLineItem> lineCaptor = ArgumentCaptor.forClass(InvoiceLineItem.class);
        verify(lineItemRepository).save(lineCaptor.capture());
        InvoiceLineItem savedLine = lineCaptor.getValue();
        assertThat(savedLine.getUnitPrice()).isEqualByComparingTo("0.00");
        assertThat(savedLine.getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(savedLine.getLinkedJob()).isEqualTo(job);
    }

    @Test
    void onJobCompleted_createsNewDraftInvoice_whenNoneExists() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        JobCompletedEvent event = new JobCompletedEvent(
                jobId, UUID.randomUUID(), clientId, new BigDecimal("150.00"), "Delivery",
                5000L, Instant.now());

        Client client = new Client();
        Job job = new Job();

        when(invoiceRepository.findFirstByClientIdAndStatusOrderByCreatedAtAsc(clientId, InvoiceStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceNumberGenerator.generate()).thenReturn("INV-2026-00001");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        listener.onJobCompleted(event);

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getClient()).isEqualTo(client);
        assertThat(invoiceCaptor.getValue().getInvoiceNumber()).isEqualTo("INV-2026-00001");

        ArgumentCaptor<InvoiceLineItem> lineCaptor = ArgumentCaptor.forClass(InvoiceLineItem.class);
        verify(lineItemRepository).save(lineCaptor.capture());
        InvoiceLineItem savedLine = lineCaptor.getValue();
        assertThat(savedLine.getDescription()).isEqualTo("Delivery");
        assertThat(savedLine.getQuantity()).isEqualByComparingTo("1");
        assertThat(savedLine.getUnitPrice()).isEqualByComparingTo("150.00");
        assertThat(savedLine.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(savedLine.getLinkedJob()).isEqualTo(job);
    }

    @Test
    void onJobCompleted_reusesExistingOpenDraft_addingSecondLine_withoutCreatingSecondInvoice() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        JobCompletedEvent event = new JobCompletedEvent(
                jobId, UUID.randomUUID(), clientId, new BigDecimal("75.00"), "Repair",
                5000L, Instant.now());

        Invoice existingDraft = new Invoice();
        Job job = new Job();

        when(invoiceRepository.findFirstByClientIdAndStatusOrderByCreatedAtAsc(clientId, InvoiceStatus.DRAFT))
                .thenReturn(Optional.of(existingDraft));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        listener.onJobCompleted(event);

        verify(invoiceRepository, never()).save(any(Invoice.class));
        verifyNoInteractions(clientRepository, invoiceNumberGenerator);

        ArgumentCaptor<InvoiceLineItem> lineCaptor = ArgumentCaptor.forClass(InvoiceLineItem.class);
        verify(lineItemRepository).save(lineCaptor.capture());
        assertThat(lineCaptor.getValue().getInvoice()).isEqualTo(existingDraft);
    }

    @Test
    void onJobCompleted_doesNotPropagate_whenRepositoryThrows() {
        UUID jobId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        JobCompletedEvent event = new JobCompletedEvent(
                jobId, UUID.randomUUID(), clientId, new BigDecimal("50.00"), "Delivery",
                5000L, Instant.now());

        doThrow(new RuntimeException("boom")).when(invoiceRepository)
                .findFirstByClientIdAndStatusOrderByCreatedAtAsc(clientId, InvoiceStatus.DRAFT);

        assertThatCode(() -> listener.onJobCompleted(event)).doesNotThrowAnyException();

        verify(lineItemRepository, never()).save(any());
    }
}

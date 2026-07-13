package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class InvoiceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllJoinFetch_initializesClient_withoutFurtherQueries() {
        Client client = persistClient("11111111A");
        Invoice invoice = persistInvoice(client, "INV-2026-00001", InvoiceStatus.DRAFT);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        Invoice fetched = result.getContent().get(0);
        assertThat(fetched.getId()).isEqualTo(invoice.getId());
        assertThat(Hibernate.isInitialized(fetched.getClient())).isTrue();
    }

    @Test
    void findAllJoinFetch_excludesSoftDeleted() {
        Client client = persistClient("22222222B");
        Invoice invoice = persistInvoice(client, "INV-2026-00002", InvoiceStatus.DRAFT);
        invoice.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(invoice);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findFirstByClientIdAndStatusOrderByCreatedAtAsc_returnsOldestOpenDraft_whenMultipleExist() {
        Client client = persistClient("33333333C");
        Invoice older = persistInvoice(client, "INV-2026-00003", InvoiceStatus.DRAFT);
        Invoice newer = persistInvoice(client, "INV-2026-00004", InvoiceStatus.DRAFT);
        entityManager.getEntityManager().clear();

        Optional<Invoice> result = invoiceRepository
                .findFirstByClientIdAndStatusOrderByCreatedAtAsc(client.getId(), InvoiceStatus.DRAFT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(older.getId());
    }

    @Test
    void findFirstByClientIdAndStatusOrderByCreatedAtAsc_ignoresNonDraftInvoices() {
        Client client = persistClient("44444444D");
        persistInvoice(client, "INV-2026-00005", InvoiceStatus.ISSUED);
        entityManager.getEntityManager().clear();

        Optional<Invoice> result = invoiceRepository
                .findFirstByClientIdAndStatusOrderByCreatedAtAsc(client.getId(), InvoiceStatus.DRAFT);

        assertThat(result).isEmpty();
    }

    @Test
    void nextInvoiceNumberSequenceValue_returnsIncreasingValues() {
        long first = invoiceRepository.nextInvoiceNumberSequenceValue();
        long second = invoiceRepository.nextInvoiceNumberSequenceValue();

        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void findUpcomingReceivables_excludesDraftAndPaidInvoices() {
        Client client = persistClient("55555555E");
        persistInvoiceWithDueDate(client, "INV-2026-00006", InvoiceStatus.DRAFT, LocalDate.now().plusDays(1));
        persistInvoiceWithDueDate(client, "INV-2026-00007", InvoiceStatus.PAID, LocalDate.now().plusDays(1));
        Invoice issued = persistInvoiceWithDueDate(
                client, "INV-2026-00008", InvoiceStatus.ISSUED, LocalDate.now().plusDays(1));
        entityManager.getEntityManager().clear();

        List<Invoice> result = invoiceRepository.findUpcomingReceivables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).extracting(Invoice::getId).containsExactly(issued.getId());
    }

    @Test
    void findUpcomingReceivables_includesAlreadyOverdueInvoices() {
        Client client = persistClient("66666666F");
        Invoice overdue = persistInvoiceWithDueDate(
                client, "INV-2026-00009", InvoiceStatus.ISSUED, LocalDate.now().minusDays(3));
        entityManager.getEntityManager().clear();

        List<Invoice> result = invoiceRepository.findUpcomingReceivables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).extracting(Invoice::getId).contains(overdue.getId());
    }

    @Test
    void findUpcomingReceivables_excludesInvoicesDueBeyondTheCutoff() {
        Client client = persistClient("77777777G");
        persistInvoiceWithDueDate(client, "INV-2026-00010", InvoiceStatus.ISSUED, LocalDate.now().plusDays(8));
        entityManager.getEntityManager().clear();

        List<Invoice> result = invoiceRepository.findUpcomingReceivables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).isEmpty();
    }

    @Test
    void findUpcomingReceivables_respectsThePageableLimit() {
        Client client = persistClient("88888888H");
        for (int i = 0; i < 3; i++) {
            persistInvoiceWithDueDate(
                    client, "INV-2026-0001" + i, InvoiceStatus.ISSUED, LocalDate.now().plusDays(i));
        }
        entityManager.getEntityManager().clear();

        List<Invoice> result = invoiceRepository.findUpcomingReceivables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void findUpcomingReceivables_ordersByDueDateAscending() {
        Client client = persistClient("99999999I");
        Invoice later = persistInvoiceWithDueDate(
                client, "INV-2026-00020", InvoiceStatus.ISSUED, LocalDate.now().plusDays(5));
        Invoice earlier = persistInvoiceWithDueDate(
                client, "INV-2026-00021", InvoiceStatus.ISSUED, LocalDate.now().minusDays(1));
        entityManager.getEntityManager().clear();

        List<Invoice> result = invoiceRepository.findUpcomingReceivables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).extracting(Invoice::getId).containsExactly(earlier.getId(), later.getId());
    }

    private Invoice persistInvoiceWithDueDate(Client client, String invoiceNumber, InvoiceStatus status, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(status);
        invoice.setDueDate(dueDate);
        invoice.setTotal(new BigDecimal("100.00"));
        return entityManager.persistAndFlush(invoice);
    }

    private Client persistClient(String taxId) {
        Client client = new Client();
        client.setName("Test Client");
        client.setTaxId(taxId);
        return entityManager.persistAndFlush(client);
    }

    private Invoice persistInvoice(Client client, String invoiceNumber, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(status);
        return entityManager.persistAndFlush(invoice);
    }
}

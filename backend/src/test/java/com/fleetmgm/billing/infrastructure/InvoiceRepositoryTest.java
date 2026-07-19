package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
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
    void findAllJoinFetch_returnsAll_whenNoFiltersProvided() {
        Client client = persistClient("11111111A");
        persistInvoice(client, "INV-2026-00001", InvoiceStatus.DRAFT);
        persistInvoice(client, "INV-2026-00040", InvoiceStatus.ISSUED);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllJoinFetch_initializesClient_withoutFurtherQueries() {
        Client client = persistClient("12121212K");
        Invoice invoice = persistInvoice(client, "INV-2026-00041", InvoiceStatus.DRAFT);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

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

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllJoinFetch_ordersByCreatedAtDescending_regardlessOfStatus() {
        Client client = persistClient("10101010J");
        Invoice oldestPaid = persistInvoice(client, "INV-2026-00030", InvoiceStatus.PAID);
        Invoice oldestPending = persistInvoice(client, "INV-2026-00031", InvoiceStatus.DRAFT);
        Invoice newestPending = persistInvoice(client, "INV-2026-00032", InvoiceStatus.ISSUED);
        Invoice newestPaid = persistInvoice(client, "INV-2026-00033", InvoiceStatus.PAID);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        // Pure chronological order — a PAID invoice (newestPaid) still ranks above an older
        // non-PAID one (oldestPending), proving status no longer influences the sort.
        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(
                newestPaid.getId(), newestPending.getId(), oldestPending.getId(), oldestPaid.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByClientId_whenProvided() {
        Client clientA = persistClient("13131313L");
        Client clientB = persistClient("14141414M");
        Invoice match = persistInvoice(clientA, "INV-2026-00042", InvoiceStatus.DRAFT);
        persistInvoice(clientB, "INV-2026-00043", InvoiceStatus.DRAFT);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                clientA.getId(), null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByInvoiceNumber_partialMatch_caseInsensitive_whenProvided() {
        Client client = persistClient("21212121T");
        Invoice match = persistInvoice(client, "INV-2026-00056", InvoiceStatus.DRAFT);
        persistInvoice(client, "INV-2026-00099", InvoiceStatus.DRAFT);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, "inv-2026-0005", null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByStatus_whenProvided() {
        Client client = persistClient("15151515N");
        Invoice draft = persistInvoice(client, "INV-2026-00044", InvoiceStatus.DRAFT);
        persistInvoice(client, "INV-2026-00045", InvoiceStatus.PAID);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                null, null, InvoiceStatus.DRAFT, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(draft.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByIssueDateRange_whenBothBoundsProvided() {
        Client client = persistClient("16161616O");
        Invoice inRange = persistInvoiceWithIssueDate(client, "INV-2026-00046", LocalDate.now());
        persistInvoiceWithIssueDate(client, "INV-2026-00047", LocalDate.now().minusDays(10));
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(inRange.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByDueDateRange_whenBothBoundsProvided() {
        Client client = persistClient("17171717P");
        Invoice inRange = persistInvoiceWithDueDate(client, "INV-2026-00048", InvoiceStatus.ISSUED, LocalDate.now().plusDays(5));
        persistInvoiceWithDueDate(client, "INV-2026-00049", InvoiceStatus.ISSUED, LocalDate.now().plusDays(20));
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null, null, null,
                LocalDate.now(), LocalDate.now().plusDays(10), null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(inRange.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByPaymentDateRange_whenBothBoundsProvided() {
        Client client = persistClient("25252525X");
        Invoice inRange = persistInvoiceWithPayment(
                client, "INV-2026-00060", LocalDate.now(), new BigDecimal("100.00"));
        persistInvoiceWithPayment(
                client, "INV-2026-00061", LocalDate.now().minusDays(10), new BigDecimal("100.00"));
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null, null, null, null, null,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(inRange.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByTotalRange_whenBothBoundsProvided() {
        Client client = persistClient("18181818Q");
        Invoice inRange = persistInvoiceWithTotal(client, "INV-2026-00050", new BigDecimal("150.00"));
        persistInvoiceWithTotal(client, "INV-2026-00051", new BigDecimal("50.00"));
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null, null, null, null, null, null, null,
                new BigDecimal("100.00"), new BigDecimal("200.00"), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(inRange.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByTotalRange_matchesDraftInvoiceByEstimatedTotal_notPersistedZero() {
        // A DRAFT invoice's persisted total/subtotal/taxAmount stay 0 until issue() computes them
        // (InvoiceService.issue()) — but InvoiceTable displays an estimated total to the user
        // (line items summed, taxed) for DRAFT rows. The total-range filter must agree with that
        // displayed estimate, not the raw persisted 0, or a DRAFT invoice the user can see visibly
        // satisfies the range silently disappears from the filtered results.
        Client client = persistClient("23232323V");
        // subtotal 300.00 + 80.00 = 380.00, * 1.21 = 459.80 estimated total — inside [400, 500].
        Invoice draftInRange = persistInvoiceWithLineItems(client, "INV-2026-00057",
                new BigDecimal("0.2100"), new BigDecimal("300.00"), new BigDecimal("80.00"));
        // subtotal 15.00, * 1.21 = 18.15 estimated total — outside [400, 500].
        persistInvoiceWithLineItems(client, "INV-2026-00058",
                new BigDecimal("0.2100"), new BigDecimal("10.00"), new BigDecimal("5.00"));
        entityManager.getEntityManager().clear();

        assertThat(draftInRange.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null, null, null, null, null, null, null,
                new BigDecimal("400.00"), new BigDecimal("500.00"), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(draftInRange.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByTotalRange_stillUsesPersistedTotal_forIssuedInvoices() {
        // Confirms the DRAFT-only CASE branch didn't accidentally change ISSUED/PAID filtering,
        // which must keep comparing against the real persisted total (an ISSUED invoice may have
        // had line items added and later removed/adjusted, so its persisted total is authoritative,
        // not a re-derivation from its current line items).
        Client client = persistClient("24242424W");
        Invoice issuedInRange = persistInvoiceWithTotal(client, "INV-2026-00059", new BigDecimal("450.00"));
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(null, null, null, null, null, null, null, null, null,
                new BigDecimal("400.00"), new BigDecimal("500.00"), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(issuedInRange.getId());
    }

    private Invoice persistInvoiceWithLineItems(Client client, String invoiceNumber, BigDecimal taxRate,
            BigDecimal... lineSubtotals) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTaxRate(taxRate);
        Invoice saved = entityManager.persistAndFlush(invoice);
        for (BigDecimal subtotal : lineSubtotals) {
            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(saved);
            lineItem.setDescription("Test line");
            lineItem.setQuantity(BigDecimal.ONE);
            lineItem.setUnitPrice(subtotal);
            lineItem.setSubtotal(subtotal);
            entityManager.persistAndFlush(lineItem);
        }
        return saved;
    }

    @Test
    void findAllJoinFetch_combinesAllFilters_whenAllProvided() {
        Client client = persistClient("19191919R");
        Client otherClient = persistClient("20202020S");

        Invoice match = persistInvoiceWithDueDate(client, "INV-2026-00052", InvoiceStatus.ISSUED, LocalDate.now().plusDays(15));
        match.setIssueDate(LocalDate.now());
        match.setTotal(new BigDecimal("150.00"));
        entityManager.persistAndFlush(match);

        // Each sibling violates exactly one of the filters below.
        Invoice wrongClient = persistInvoiceWithDueDate(otherClient, "INV-2026-00053", InvoiceStatus.ISSUED, LocalDate.now().plusDays(15));
        wrongClient.setIssueDate(LocalDate.now());
        wrongClient.setTotal(new BigDecimal("150.00"));
        entityManager.persistAndFlush(wrongClient);

        Invoice wrongStatus = persistInvoiceWithDueDate(client, "INV-2026-00054", InvoiceStatus.PAID, LocalDate.now().plusDays(15));
        wrongStatus.setIssueDate(LocalDate.now());
        wrongStatus.setTotal(new BigDecimal("150.00"));
        entityManager.persistAndFlush(wrongStatus);

        Invoice wrongTotal = persistInvoiceWithDueDate(client, "INV-2026-00055", InvoiceStatus.ISSUED, LocalDate.now().plusDays(15));
        wrongTotal.setIssueDate(LocalDate.now());
        wrongTotal.setTotal(new BigDecimal("999.00"));
        entityManager.persistAndFlush(wrongTotal);
        entityManager.getEntityManager().clear();

        Page<Invoice> result = invoiceRepository.findAllJoinFetch(
                client.getId(), null, InvoiceStatus.ISSUED,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(20),
                null, null,
                new BigDecimal("100.00"), new BigDecimal("200.00"), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Invoice::getId).containsExactly(match.getId());
    }

    private Invoice persistInvoiceWithIssueDate(Client client, String invoiceNumber, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(issueDate);
        return entityManager.persistAndFlush(invoice);
    }

    private Invoice persistInvoiceWithPayment(
            Client client, String invoiceNumber, LocalDate paymentDate, BigDecimal subtotal) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(paymentDate);
        invoice.setSubtotal(subtotal);
        return entityManager.persistAndFlush(invoice);
    }

    private Invoice persistInvoiceWithTotal(Client client, String invoiceNumber, BigDecimal total) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setTotal(total);
        return entityManager.persistAndFlush(invoice);
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

    @Test
    void sumSubtotalByPaymentDateBetween_sumsOnlyPaidInvoices_withinRange() {
        Client client = persistClient("10101010J");
        persistInvoiceWithPayment(client, "INV-2026-00030", InvoiceStatus.PAID,
                LocalDate.of(2026, 6, 15), new BigDecimal("1000.00"));
        persistInvoiceWithPayment(client, "INV-2026-00031", InvoiceStatus.PAID,
                LocalDate.of(2026, 6, 20), new BigDecimal("500.00"));
        // ISSUED (not yet paid) and PAID-but-outside-range must not count.
        persistInvoiceWithDueDate(client, "INV-2026-00032", InvoiceStatus.ISSUED, LocalDate.now().plusDays(5));
        persistInvoiceWithPayment(client, "INV-2026-00033", InvoiceStatus.PAID,
                LocalDate.of(2026, 7, 1), new BigDecimal("999.00"));
        entityManager.getEntityManager().clear();

        BigDecimal result = invoiceRepository.sumSubtotalByPaymentDateBetween(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(result).isEqualByComparingTo("1500.00");
    }

    @Test
    void sumSubtotalByPaymentDateBetween_returnsZero_notNull_whenNoPaidInvoicesInRange() {
        BigDecimal result = invoiceRepository.sumSubtotalByPaymentDateBetween(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Invoice persistInvoiceWithPayment(
            Client client, String invoiceNumber, InvoiceStatus status, LocalDate paymentDate, BigDecimal subtotal) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(status);
        invoice.setPaymentDate(paymentDate);
        invoice.setSubtotal(subtotal);
        return entityManager.persistAndFlush(invoice);
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

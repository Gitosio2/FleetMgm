package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class LineItemRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    LineItemRepository lineItemRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllByVehicleIdAndPeriod_returnsLineItems_linkedToVehicleThroughJob() {
        Vehicle vehicle = persistVehicle("1111AAA");
        Client client = persistClient("11111111A");
        Job job = persistJob(vehicle);
        Invoice invoice = persistInvoice(client, "INV-2026-00001", LocalDate.of(2026, 7, 5));
        InvoiceLineItem lineItem = persistLineItem(invoice, job, new BigDecimal("500.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).extracting(InvoiceLineItem::getId).containsExactly(lineItem.getId());
    }

    @Test
    void findAllByVehicleIdAndPeriod_excludesLineItems_forOtherVehicles() {
        Vehicle vehicleA = persistVehicle("2222BBB");
        Vehicle vehicleB = persistVehicle("3333CCC");
        Client client = persistClient("22222222B");
        Job jobA = persistJob(vehicleA);
        Job jobB = persistJob(vehicleB);
        Invoice invoice = persistInvoice(client, "INV-2026-00002", LocalDate.of(2026, 7, 5));
        persistLineItem(invoice, jobA, new BigDecimal("100.00"));
        persistLineItem(invoice, jobB, new BigDecimal("200.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(vehicleA.getId(), null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void findAllByVehicleIdAndPeriod_excludesLineItems_withoutLinkedJob() {
        Vehicle vehicle = persistVehicle("4444DDD");
        Client client = persistClient("33333333C");
        Invoice invoice = persistInvoice(client, "INV-2026-00003", LocalDate.of(2026, 7, 5));
        persistLineItem(invoice, null, new BigDecimal("999.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByVehicleIdAndPeriod_narrowsByFrom_whenProvided() {
        Vehicle vehicle = persistVehicle("5555EEE");
        Client client = persistClient("44444444D");
        Job jobInRange = persistJob(vehicle);
        Job jobBeforeRange = persistJob(vehicle);
        Invoice invoiceInRange = persistInvoice(client, "INV-2026-00004", LocalDate.of(2026, 3, 10));
        Invoice invoiceBeforeRange = persistInvoice(client, "INV-2025-00099", LocalDate.of(2025, 3, 10));
        InvoiceLineItem match = persistLineItem(invoiceInRange, jobInRange, new BigDecimal("150.00"));
        persistLineItem(invoiceBeforeRange, jobBeforeRange, new BigDecimal("250.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(
                vehicle.getId(), LocalDate.of(2026, 1, 1), null);

        assertThat(result).extracting(InvoiceLineItem::getId).containsExactly(match.getId());
    }

    @Test
    void findAllByVehicleIdAndPeriod_narrowsByTo_whenProvided() {
        Vehicle vehicle = persistVehicle("6666FFF");
        Client client = persistClient("55555555E");
        Job jobJuly = persistJob(vehicle);
        Job jobAugust = persistJob(vehicle);
        Invoice invoiceJuly = persistInvoice(client, "INV-2026-00005", LocalDate.of(2026, 7, 20));
        Invoice invoiceAugust = persistInvoice(client, "INV-2026-00006", LocalDate.of(2026, 8, 20));
        InvoiceLineItem match = persistLineItem(invoiceJuly, jobJuly, new BigDecimal("350.00"));
        persistLineItem(invoiceAugust, jobAugust, new BigDecimal("450.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(
                vehicle.getId(), null, LocalDate.of(2026, 7, 31));

        assertThat(result).extracting(InvoiceLineItem::getId).containsExactly(match.getId());
    }

    @Test
    void findAllByVehicleIdAndPeriod_combinesFromAndTo_whenBothProvided() {
        Vehicle vehicle = persistVehicle("7777GGG");
        Client client = persistClient("66666666F");
        Job jobMatch = persistJob(vehicle);
        Job jobAfterRange = persistJob(vehicle);
        Job jobBeforeRange = persistJob(vehicle);
        Invoice invoiceMatch = persistInvoice(client, "INV-2026-00007", LocalDate.of(2026, 7, 1));
        Invoice invoiceAfterRange = persistInvoice(client, "INV-2026-00008", LocalDate.of(2026, 8, 1));
        Invoice invoiceBeforeRange = persistInvoice(client, "INV-2025-00098", LocalDate.of(2025, 7, 1));
        InvoiceLineItem match = persistLineItem(invoiceMatch, jobMatch, new BigDecimal("111.00"));
        persistLineItem(invoiceAfterRange, jobAfterRange, new BigDecimal("222.00"));
        persistLineItem(invoiceBeforeRange, jobBeforeRange, new BigDecimal("333.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(
                vehicle.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 31));

        assertThat(result).extracting(InvoiceLineItem::getId).containsExactly(match.getId());
    }

    @Test
    void findAllByVehicleIdAndPeriod_excludesSoftDeletedInvoices() {
        Vehicle vehicle = persistVehicle("8888HHH");
        Client client = persistClient("77777777G");
        Job job = persistJob(vehicle);
        Invoice invoice = persistInvoice(client, "INV-2026-00009", LocalDate.of(2026, 7, 5));
        persistLineItem(invoice, job, new BigDecimal("100.00"));
        invoice.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(invoice);
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByVehicleIdAndPeriod_excludesLineItems_fromCancelledInvoice() {
        // Mirrors ProfitabilityRepositoryTest.findProfitabilityByVehicle_excludesRevenue_fromCancelledInvoice:
        // InvoiceService.delete() cancels an ISSUED invoice by flipping status to CANCELLED without
        // setting deletedAt (the fiscal number must stay visible), so deleted_at IS NULL alone must
        // not be enough to keep its line items showing up in this vehicle revenue history.
        Vehicle vehicle = persistVehicle("9999III");
        Client client = persistClient("10101010J");
        Job job = persistJob(vehicle);
        Invoice cancelledInvoice = new Invoice();
        cancelledInvoice.setClient(client);
        cancelledInvoice.setInvoiceNumber("INV-2026-00013");
        cancelledInvoice.setStatus(InvoiceStatus.CANCELLED);
        cancelledInvoice.setIssueDate(LocalDate.of(2026, 7, 5));
        entityManager.persistAndFlush(cancelledInvoice);
        persistLineItem(cancelledInvoice, job, new BigDecimal("777.00"));
        entityManager.getEntityManager().clear();

        List<InvoiceLineItem> result = lineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdAndInvoiceId_returnsLineItem_whenItBelongsToTheGivenInvoice() {
        Client client = persistClient("88888888H");
        Invoice invoice = persistInvoice(client, "INV-2026-00010", LocalDate.of(2026, 7, 5));
        InvoiceLineItem lineItem = persistLineItem(invoice, null, new BigDecimal("100.00"));
        entityManager.getEntityManager().clear();

        var result = lineItemRepository.findByIdAndInvoiceId(lineItem.getId(), invoice.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(lineItem.getId());
    }

    @Test
    void findByIdAndInvoiceId_returnsEmpty_whenLineItemBelongsToADifferentInvoice() {
        Client client = persistClient("99999999I");
        Invoice ownerInvoice = persistInvoice(client, "INV-2026-00011", LocalDate.of(2026, 7, 5));
        Invoice otherInvoice = persistInvoice(client, "INV-2026-00012", LocalDate.of(2026, 7, 6));
        InvoiceLineItem lineItem = persistLineItem(ownerInvoice, null, new BigDecimal("100.00"));
        entityManager.getEntityManager().clear();

        var result = lineItemRepository.findByIdAndInvoiceId(lineItem.getId(), otherInvoice.getId());

        assertThat(result).isEmpty();
    }

    private Vehicle persistVehicle(String licensePlate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake("Toyota");
        vehicle.setModel("Hilux");
        vehicle.setYear(2020);
        vehicle.setLicensePlate(licensePlate);
        return entityManager.persistAndFlush(vehicle);
    }

    private Client persistClient(String taxId) {
        Client client = new Client();
        client.setName("Test Client");
        client.setTaxId(taxId);
        return entityManager.persistAndFlush(client);
    }

    private Job persistJob(Vehicle vehicle) {
        Job job = new Job();
        job.setTitle("Test Job");
        job.setVehicle(vehicle);
        job.setStatus(JobStatus.COMPLETED);
        job.setOriginLocation("Origin");
        job.setDestinationLocation("Destination");
        return entityManager.persistAndFlush(job);
    }

    private Invoice persistInvoice(Client client, String invoiceNumber, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(issueDate);
        return entityManager.persistAndFlush(invoice);
    }

    private InvoiceLineItem persistLineItem(Invoice invoice, Job linkedJob, BigDecimal subtotal) {
        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription("Test line item");
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(subtotal);
        lineItem.setSubtotal(subtotal);
        lineItem.setLinkedJob(linkedJob);
        return entityManager.persistAndFlush(lineItem);
    }
}

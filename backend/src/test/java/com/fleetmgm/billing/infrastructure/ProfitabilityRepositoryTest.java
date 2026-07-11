package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class ProfitabilityRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ProfitabilityRepository profitabilityRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findProfitabilityByVehicle_computesRevenueAndCosts_forHandVerifiedExample() {
        Vehicle vehicle = persistVehicle("1111AAA");
        Client client = persistClient("11111111A");

        // Revenue: one Job with an InvoiceLineItem linked via linkedJob -> subtotal 500.00
        Job job = persistJob(vehicle, JobStatus.COMPLETED);
        Invoice invoice = persistInvoice(client, "INV-2026-00001", InvoiceStatus.ISSUED);
        persistLineItem(invoice, new BigDecimal("500.00"), job, null);

        // Costs: one MaintenanceRecord with cost 120.50 + one SupplierInvoice with total 80.25
        persistMaintenanceRecord(vehicle, new BigDecimal("120.50"));
        persistSupplierInvoice(vehicle, new BigDecimal("80.25"));

        entityManager.getEntityManager().clear();

        Page<VehicleProfitabilityProjection> result =
                profitabilityRepository.findProfitabilityByVehicle(PageRequest.of(0, 20));

        VehicleProfitabilityProjection projection = findByVehicleId(result, vehicle.getId());
        assertThat(projection.getVehicleLicensePlate()).isEqualTo("1111AAA");
        assertThat(projection.getVehicleMake()).isEqualTo("Toyota");
        assertThat(projection.getVehicleModel()).isEqualTo("Hilux");
        assertThat(projection.getRevenue()).isEqualByComparingTo("500.00");
        assertThat(projection.getCosts()).isEqualByComparingTo("200.75");
    }

    @Test
    void findProfitabilityByVehicle_returnsZero_notNull_whenNoRelatedRecordsExist() {
        Vehicle vehicle = persistVehicle("2222BBB");
        entityManager.getEntityManager().clear();

        Page<VehicleProfitabilityProjection> result =
                profitabilityRepository.findProfitabilityByVehicle(PageRequest.of(0, 20));

        VehicleProfitabilityProjection projection = findByVehicleId(result, vehicle.getId());
        assertThat(projection.getRevenue()).isNotNull();
        assertThat(projection.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(projection.getCosts()).isNotNull();
        assertThat(projection.getCosts()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findProfitabilityByVehicle_excludesLineItemsLinkedOnlyViaMaintenance_notJob() {
        Vehicle vehicle = persistVehicle("3333CCC");
        Client client = persistClient("22222222B");
        MaintenanceRecord maintenanceRecord = persistMaintenanceRecord(vehicle, new BigDecimal("50.00"));
        Invoice invoice = persistInvoice(client, "INV-2026-00002", InvoiceStatus.ISSUED);
        // Linked via linkedMaintenance only, NOT linkedJob — must NOT count toward revenue.
        persistLineItem(invoice, new BigDecimal("999.00"), null, maintenanceRecord);

        entityManager.getEntityManager().clear();

        Page<VehicleProfitabilityProjection> result =
                profitabilityRepository.findProfitabilityByVehicle(PageRequest.of(0, 20));

        VehicleProfitabilityProjection projection = findByVehicleId(result, vehicle.getId());
        assertThat(projection.getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        // The maintenance cost itself still counts toward costs.
        assertThat(projection.getCosts()).isEqualByComparingTo("50.00");
    }

    @Test
    void findProfitabilityByVehicle_attributesSharedSupplierInvoiceCosts_viaLineItems() {
        Vehicle vehicleA = persistVehicle("4444DDD");
        Vehicle vehicleB = persistVehicle("5555EEE");

        // Shared invoice covering multiple vehicles: no single vehicle_id on the invoice itself,
        // so the per-vehicle cost breakdown lives entirely in its line items.
        SupplierInvoice sharedInvoice = persistSupplierInvoice(null, new BigDecimal("100.00"));
        persistSupplierInvoiceLineItem(sharedInvoice, vehicleA, new BigDecimal("60.00"));
        persistSupplierInvoiceLineItem(sharedInvoice, vehicleB, new BigDecimal("40.00"));

        entityManager.getEntityManager().clear();

        Page<VehicleProfitabilityProjection> result =
                profitabilityRepository.findProfitabilityByVehicle(PageRequest.of(0, 20));

        VehicleProfitabilityProjection projectionA = findByVehicleId(result, vehicleA.getId());
        VehicleProfitabilityProjection projectionB = findByVehicleId(result, vehicleB.getId());
        assertThat(projectionA.getCosts()).isEqualByComparingTo("60.00");
        assertThat(projectionB.getCosts()).isEqualByComparingTo("40.00");
    }

    @Test
    void findProfitabilityByVehicle_doesNotDoubleCount_singleVehicleInvoiceWithLineItems() {
        Vehicle vehicle = persistVehicle("6666FFF");

        // Single-vehicle invoice (vehicle_id set) that ALSO carries a line item tagged to the
        // same vehicle, with a subtotal matching the invoice total — if the fix double-counts,
        // this asserts 400.00 would surface instead of the correct 200.00.
        SupplierInvoice invoice = persistSupplierInvoice(vehicle, new BigDecimal("200.00"));
        persistSupplierInvoiceLineItem(invoice, vehicle, new BigDecimal("200.00"));

        entityManager.getEntityManager().clear();

        Page<VehicleProfitabilityProjection> result =
                profitabilityRepository.findProfitabilityByVehicle(PageRequest.of(0, 20));

        VehicleProfitabilityProjection projection = findByVehicleId(result, vehicle.getId());
        assertThat(projection.getCosts()).isEqualByComparingTo("200.00");
    }

    private VehicleProfitabilityProjection findByVehicleId(Page<VehicleProfitabilityProjection> page, java.util.UUID vehicleId) {
        List<VehicleProfitabilityProjection> matches = page.getContent().stream()
                .filter(p -> p.getVehicleId().equals(vehicleId))
                .toList();
        assertThat(matches).hasSize(1);
        return matches.get(0);
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

    private Job persistJob(Vehicle vehicle, JobStatus status) {
        Job job = new Job();
        job.setTitle("Test Job");
        job.setVehicle(vehicle);
        job.setStatus(status);
        job.setOriginLocation("Origin");
        job.setDestinationLocation("Destination");
        return entityManager.persistAndFlush(job);
    }

    private Invoice persistInvoice(Client client, String invoiceNumber, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(status);
        return entityManager.persistAndFlush(invoice);
    }

    private InvoiceLineItem persistLineItem(Invoice invoice, BigDecimal subtotal, Job linkedJob, MaintenanceRecord linkedMaintenance) {
        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription("Test line item");
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(subtotal);
        lineItem.setSubtotal(subtotal);
        lineItem.setLinkedJob(linkedJob);
        lineItem.setLinkedMaintenance(linkedMaintenance);
        return entityManager.persistAndFlush(lineItem);
    }

    private MaintenanceRecord persistMaintenanceRecord(Vehicle vehicle, BigDecimal cost) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setType("Oil change");
        record.setCost(cost);
        return entityManager.persistAndFlush(record);
    }

    private SupplierInvoice persistSupplierInvoice(Vehicle vehicle, BigDecimal total) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplierName("Test Supplier");
        invoice.setCategory(ExpenseCategory.MAINTENANCE);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setVehicle(vehicle);
        invoice.setSubtotal(total);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotal(total);
        return entityManager.persistAndFlush(invoice);
    }

    private SupplierInvoiceLineItem persistSupplierInvoiceLineItem(SupplierInvoice invoice, Vehicle vehicle, BigDecimal subtotal) {
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription("Test supplier line item");
        lineItem.setQuantity(BigDecimal.ONE);
        lineItem.setUnitPrice(subtotal);
        lineItem.setSubtotal(subtotal);
        lineItem.setVehicle(vehicle);
        return entityManager.persistAndFlush(lineItem);
    }
}

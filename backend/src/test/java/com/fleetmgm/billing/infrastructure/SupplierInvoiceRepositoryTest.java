package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceStatus;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class SupplierInvoiceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SupplierInvoiceRepository supplierInvoiceRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllJoinFetch_returnsAll_whenNoFiltersProvided() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("1111AAA");
        persistInvoice(supplier, vehicle, ExpenseCategory.MAINTENANCE);
        persistInvoice(supplier, vehicle, ExpenseCategory.FUEL);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllJoinFetch_narrowsByVehicleId_whenProvided() {
        Supplier supplier = persistSupplier();
        Vehicle vehicleA = persistVehicle("2222BBB");
        Vehicle vehicleB = persistVehicle("3333CCC");
        SupplierInvoice invoiceA = persistInvoice(supplier, vehicleA, ExpenseCategory.MAINTENANCE);
        persistInvoice(supplier, vehicleB, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(vehicleA.getId(), null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(invoiceA.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByCategory_whenProvided() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("4444DDD");
        SupplierInvoice fuelInvoice = persistInvoice(supplier, vehicle, ExpenseCategory.FUEL);
        persistInvoice(supplier, vehicle, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(null, ExpenseCategory.FUEL, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(fuelInvoice.getId());
    }

    @Test
    void findAllJoinFetch_combinesBothFilters_whenBothProvided() {
        Supplier supplier = persistSupplier();
        Vehicle vehicleA = persistVehicle("5555EEE");
        Vehicle vehicleB = persistVehicle("6666FFF");
        SupplierInvoice match = persistInvoice(supplier, vehicleA, ExpenseCategory.FUEL);
        persistInvoice(supplier, vehicleA, ExpenseCategory.MAINTENANCE);
        persistInvoice(supplier, vehicleB, ExpenseCategory.FUEL);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(vehicleA.getId(), ExpenseCategory.FUEL, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(match.getId());
    }

    @Test
    void findAllJoinFetch_initializesVehicleAndSupplier_withoutFurtherQueries() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("7777GGG");
        persistInvoice(supplier, vehicle, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        SupplierInvoice fetched = result.getContent().get(0);
        assertThat(Hibernate.isInitialized(fetched.getVehicle())).isTrue();
        assertThat(Hibernate.isInitialized(fetched.getSupplier())).isTrue();
    }

    @Test
    void findAllJoinFetch_excludesSoftDeleted() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("8888HHH");
        SupplierInvoice invoice = persistInvoice(supplier, vehicle, ExpenseCategory.MAINTENANCE);
        invoice.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(invoice);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void sumTotalByInvoiceDateBetween_sumsOnlyInvoicesInRange() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("9999III");
        persistInvoiceWithTotal(supplier, vehicle, new BigDecimal("121.00"), LocalDate.now());
        persistInvoiceWithTotal(supplier, vehicle, new BigDecimal("60.50"), LocalDate.now());
        // Outside the queried range — must not be included in the sum.
        persistInvoiceWithTotal(supplier, vehicle, new BigDecimal("999.00"), LocalDate.now().minusMonths(2));
        entityManager.getEntityManager().clear();

        BigDecimal total = supplierInvoiceRepository.sumTotalByInvoiceDateBetween(
                LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        assertThat(total).isEqualByComparingTo("181.50");
    }

    @Test
    void sumTotalByInvoiceDateBetween_returnsZero_whenNoInvoicesInRange() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("0000JJJ");
        persistInvoiceWithTotal(supplier, vehicle, new BigDecimal("121.00"), LocalDate.now().minusMonths(3));
        entityManager.getEntityManager().clear();

        BigDecimal total = supplierInvoiceRepository.sumTotalByInvoiceDateBetween(
                LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findUpcomingPayables_excludesPaidInvoices() {
        Supplier supplier = persistSupplier();
        persistInvoiceWithDueDate(supplier, "F-2026-0001", SupplierInvoiceStatus.PAID, LocalDate.now().plusDays(1));
        SupplierInvoice pending = persistInvoiceWithDueDate(
                supplier, "F-2026-0002", SupplierInvoiceStatus.PENDING, LocalDate.now().plusDays(1));
        entityManager.getEntityManager().clear();

        List<SupplierInvoice> result = supplierInvoiceRepository.findUpcomingPayables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).extracting(SupplierInvoice::getId).containsExactly(pending.getId());
    }

    @Test
    void findUpcomingPayables_includesAlreadyOverdueInvoices() {
        Supplier supplier = persistSupplier();
        SupplierInvoice overdue = persistInvoiceWithDueDate(
                supplier, "F-2026-0003", SupplierInvoiceStatus.PENDING, LocalDate.now().minusDays(3));
        entityManager.getEntityManager().clear();

        List<SupplierInvoice> result = supplierInvoiceRepository.findUpcomingPayables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).extracting(SupplierInvoice::getId).contains(overdue.getId());
    }

    @Test
    void findUpcomingPayables_excludesInvoicesDueBeyondTheCutoff() {
        Supplier supplier = persistSupplier();
        persistInvoiceWithDueDate(supplier, "F-2026-0004", SupplierInvoiceStatus.PENDING, LocalDate.now().plusDays(8));
        entityManager.getEntityManager().clear();

        List<SupplierInvoice> result = supplierInvoiceRepository.findUpcomingPayables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 5));

        assertThat(result).isEmpty();
    }

    @Test
    void findUpcomingPayables_respectsThePageableLimit() {
        Supplier supplier = persistSupplier();
        for (int i = 0; i < 3; i++) {
            persistInvoiceWithDueDate(
                    supplier, "F-2026-010" + i, SupplierInvoiceStatus.PENDING, LocalDate.now().plusDays(i));
        }
        entityManager.getEntityManager().clear();

        List<SupplierInvoice> result = supplierInvoiceRepository.findUpcomingPayables(
                LocalDate.now().plusDays(7), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    private SupplierInvoice persistInvoiceWithDueDate(
            Supplier supplier, String supplierInvoiceNumber, SupplierInvoiceStatus status, LocalDate dueDate) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setSupplierInvoiceNumber(supplierInvoiceNumber);
        invoice.setCategory(ExpenseCategory.MAINTENANCE);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(dueDate);
        invoice.setStatus(status);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("21.00"));
        invoice.setTotal(new BigDecimal("121.00"));
        return entityManager.persistAndFlush(invoice);
    }

    private SupplierInvoice persistInvoiceWithTotal(Supplier supplier, Vehicle vehicle, BigDecimal total, LocalDate invoiceDate) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setCategory(ExpenseCategory.MAINTENANCE);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setVehicle(vehicle);
        invoice.setSubtotal(total);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotal(total);
        return entityManager.persistAndFlush(invoice);
    }

    private Supplier persistSupplier() {
        Supplier supplier = new Supplier();
        supplier.setName("Test Supplier");
        return entityManager.persistAndFlush(supplier);
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

    private SupplierInvoice persistInvoice(Supplier supplier, Vehicle vehicle, ExpenseCategory category) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setCategory(category);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setVehicle(vehicle);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("21.00"));
        invoice.setTotal(new BigDecimal("121.00"));
        return entityManager.persistAndFlush(invoice);
    }
}

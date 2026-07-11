package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
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
        Vehicle vehicle = persistVehicle("1111AAA");
        persistInvoice(vehicle, ExpenseCategory.MAINTENANCE);
        persistInvoice(vehicle, ExpenseCategory.FUEL);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllJoinFetch_narrowsByVehicleId_whenProvided() {
        Vehicle vehicleA = persistVehicle("2222BBB");
        Vehicle vehicleB = persistVehicle("3333CCC");
        SupplierInvoice invoiceA = persistInvoice(vehicleA, ExpenseCategory.MAINTENANCE);
        persistInvoice(vehicleB, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(vehicleA.getId(), null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(invoiceA.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByCategory_whenProvided() {
        Vehicle vehicle = persistVehicle("4444DDD");
        SupplierInvoice fuelInvoice = persistInvoice(vehicle, ExpenseCategory.FUEL);
        persistInvoice(vehicle, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(null, ExpenseCategory.FUEL, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(fuelInvoice.getId());
    }

    @Test
    void findAllJoinFetch_combinesBothFilters_whenBothProvided() {
        Vehicle vehicleA = persistVehicle("5555EEE");
        Vehicle vehicleB = persistVehicle("6666FFF");
        SupplierInvoice match = persistInvoice(vehicleA, ExpenseCategory.FUEL);
        persistInvoice(vehicleA, ExpenseCategory.MAINTENANCE);
        persistInvoice(vehicleB, ExpenseCategory.FUEL);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository
                .findAllJoinFetch(vehicleA.getId(), ExpenseCategory.FUEL, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(match.getId());
    }

    @Test
    void findAllJoinFetch_initializesVehicle_withoutFurtherQueries() {
        Vehicle vehicle = persistVehicle("7777GGG");
        persistInvoice(vehicle, ExpenseCategory.MAINTENANCE);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        SupplierInvoice fetched = result.getContent().get(0);
        assertThat(Hibernate.isInitialized(fetched.getVehicle())).isTrue();
    }

    @Test
    void findAllJoinFetch_excludesSoftDeleted() {
        Vehicle vehicle = persistVehicle("8888HHH");
        SupplierInvoice invoice = persistInvoice(vehicle, ExpenseCategory.MAINTENANCE);
        invoice.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(invoice);
        entityManager.getEntityManager().clear();

        Page<SupplierInvoice> result = supplierInvoiceRepository.findAllJoinFetch(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
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

    private SupplierInvoice persistInvoice(Vehicle vehicle, ExpenseCategory category) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplierName("Test Supplier");
        invoice.setCategory(category);
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setVehicle(vehicle);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(new BigDecimal("21.00"));
        invoice.setTotal(new BigDecimal("121.00"));
        return entityManager.persistAndFlush(invoice);
    }
}

package com.fleetmgm.billing.infrastructure;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.supplier.domain.Supplier;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

// Vehicle profitability panel's merged "Historial de gastos" list (Hito 45) — the split-invoice
// complement to SupplierInvoiceRepositoryTest.findAllByVehicleIdAndPeriod. Same scaffold as that
// class (@DataJpaTest + real Postgres via Testcontainers, not H2 — CLAUDE.md repository test rule).
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class SupplierInvoiceLineItemRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllByVehicleIdAndPeriod_returnsSplitLineItem_tiedToVehicle() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("2121AAA");
        // Bulk invoice split across vehicles — no header-level vehicle, so the line item is the
        // only place this vehicle's share of the cost is attributed.
        SupplierInvoice bulkInvoice = persistInvoice(supplier, null);
        SupplierInvoiceLineItem lineItem = persistLineItem(bulkInvoice, vehicle, new BigDecimal("60.00"));
        entityManager.getEntityManager().clear();

        List<SupplierInvoiceLineItem> result =
                supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).extracting(SupplierInvoiceLineItem::getId).containsExactly(lineItem.getId());
    }

    // The critical double-count guard: a line item tied to the vehicle whose PARENT invoice is
    // ALSO directly tied to a vehicle (any vehicle) must NOT appear here — that invoice's total is
    // already counted whole via SupplierInvoiceRepository.findAllByVehicleIdAndPeriod, mirroring
    // ProfitabilityRepository's "si2.vehicle_id IS NULL" subquery guard exactly. Without this guard
    // ProfitabilityService.getExpensesByVehicle would double-count against "Totales" -> "Gastos".
    @Test
    void findAllByVehicleIdAndPeriod_excludesLineItem_whenParentInvoiceHasHeaderVehicle() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("2222BBB");
        SupplierInvoice invoiceWithHeaderVehicle = persistInvoice(supplier, vehicle);
        persistLineItem(invoiceWithHeaderVehicle, vehicle, new BigDecimal("60.00"));
        entityManager.getEntityManager().clear();

        List<SupplierInvoiceLineItem> result =
                supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicle.getId(), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByVehicleIdAndPeriod_excludesLineItems_forOtherVehicles() {
        Supplier supplier = persistSupplier();
        Vehicle vehicleA = persistVehicle("2323CCC");
        Vehicle vehicleB = persistVehicle("2424DDD");
        SupplierInvoice bulkInvoice = persistInvoice(supplier, null);
        persistLineItem(bulkInvoice, vehicleB, new BigDecimal("30.00"));
        entityManager.getEntityManager().clear();

        List<SupplierInvoiceLineItem> result =
                supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicleA.getId(), null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByVehicleIdAndPeriod_narrowsByParentInvoiceDateRange_whenBothBoundsProvided() {
        Supplier supplier = persistSupplier();
        Vehicle vehicle = persistVehicle("2525EEE");
        SupplierInvoice inRangeInvoice = persistInvoiceWithDate(supplier, null, LocalDate.now());
        SupplierInvoiceLineItem inRange = persistLineItem(inRangeInvoice, vehicle, new BigDecimal("40.00"));
        SupplierInvoice outOfRangeInvoice =
                persistInvoiceWithDate(supplier, null, LocalDate.now().minusMonths(2));
        persistLineItem(outOfRangeInvoice, vehicle, new BigDecimal("40.00"));
        entityManager.getEntityManager().clear();

        List<SupplierInvoiceLineItem> result = supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(
                vehicle.getId(), LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));

        assertThat(result).extracting(SupplierInvoiceLineItem::getId).containsExactly(inRange.getId());
    }

    private SupplierInvoiceLineItem persistLineItem(SupplierInvoice invoice, Vehicle vehicle, BigDecimal subtotal) {
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription("Gasoil");
        lineItem.setQuantity(BigDecimal.TEN);
        lineItem.setUnitPrice(subtotal.divide(BigDecimal.TEN));
        lineItem.setSubtotal(subtotal);
        lineItem.setVehicle(vehicle);
        return entityManager.persistAndFlush(lineItem);
    }

    private SupplierInvoice persistInvoice(Supplier supplier, Vehicle vehicle) {
        return persistInvoiceWithDate(supplier, vehicle, LocalDate.now());
    }

    private SupplierInvoice persistInvoiceWithDate(Supplier supplier, Vehicle vehicle, LocalDate invoiceDate) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setCategory(ExpenseCategory.FUEL);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setVehicle(vehicle);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotal(new BigDecimal("100.00"));
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
}

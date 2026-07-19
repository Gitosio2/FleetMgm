package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
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
class MaintenanceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MaintenanceRepository maintenanceRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllJoinFetch_initializesVehicleAndTechnician_withoutFurtherQueries() {
        Worker technician = persistTechnician("11111111A");
        Vehicle vehicle = persistVehicle("1111AAA");
        MaintenanceRecord record = persistMaintenance(vehicle, technician, MaintenanceStatus.SCHEDULED);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository
                .findAllJoinFetch(null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        MaintenanceRecord fetched = result.getContent().get(0);
        assertThat(fetched.getId()).isEqualTo(record.getId());
        assertThat(Hibernate.isInitialized(fetched.getVehicle())).isTrue();
        assertThat(Hibernate.isInitialized(fetched.getTechnician())).isTrue();
    }

    @Test
    void findAllJoinFetch_excludesSoftDeleted() {
        Vehicle vehicle = persistVehicle("2222BBB");
        MaintenanceRecord record = persistMaintenance(vehicle, null, MaintenanceStatus.SCHEDULED);
        record.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(record);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository
                .findAllJoinFetch(null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllJoinFetch_narrowsByVehicleId_whenProvided() {
        Vehicle vehicleA = persistVehicle("7777GGG");
        Vehicle vehicleB = persistVehicle("8888HHH");
        MaintenanceRecord recordA = persistMaintenance(vehicleA, null, MaintenanceStatus.SCHEDULED);
        persistMaintenance(vehicleB, null, MaintenanceStatus.SCHEDULED);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                vehicleA.getId(), null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(recordA.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByYear_whenProvided() {
        Vehicle vehicle = persistVehicle("9999III");
        MaintenanceRecord thisYear = persistMaintenanceWithCost(vehicle, new BigDecimal("100.00"), LocalDate.now());
        persistMaintenanceWithCost(vehicle, new BigDecimal("50.00"), LocalDate.now().minusYears(1));
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, LocalDate.now().getYear(), null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(thisYear.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByMonth_whenProvided() {
        Vehicle vehicle = persistVehicle("0000JJJ");
        MaintenanceRecord thisMonth = persistMaintenanceWithCost(vehicle, new BigDecimal("100.00"), LocalDate.now());
        persistMaintenanceWithCost(vehicle, new BigDecimal("50.00"), LocalDate.now().minusMonths(2));
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, LocalDate.now().getMonthValue(), null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(thisMonth.getId());
    }

    @Test
    void findAllJoinFetch_combinesAllFilters_whenAllProvided() {
        Vehicle vehicleA = persistVehicle("1234AAB");
        Vehicle vehicleB = persistVehicle("5678BBC");
        MaintenanceRecord match = persistMaintenanceWithCost(vehicleA, new BigDecimal("100.00"), LocalDate.now());
        persistMaintenanceWithCost(vehicleA, new BigDecimal("50.00"), LocalDate.now().minusMonths(2));
        persistMaintenanceWithCost(vehicleB, new BigDecimal("75.00"), LocalDate.now());
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                vehicleA.getId(), LocalDate.now().getYear(), LocalDate.now().getMonthValue(),
                null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(match.getId());
    }

    // --- new filters: type/category/status/technicianId/costFrom/costTo ---

    @Test
    void findAllJoinFetch_narrowsByType_partialMatch_caseInsensitive_whenProvided() {
        Vehicle vehicle = persistVehicle("7333CCC");
        MaintenanceRecord match = persistMaintenanceWithType(vehicle, "Oil change");
        persistMaintenanceWithType(vehicle, "Brake repair");
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, null, "oil", null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(MaintenanceRecord::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByCategory_whenProvided() {
        Vehicle vehicle = persistVehicle("7444DDD");
        MaintenanceRecord match = persistMaintenanceWithCategory(vehicle, MaintenanceCategory.CORRECTIVE);
        persistMaintenanceWithCategory(vehicle, MaintenanceCategory.PREVENTIVE);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, null, null, MaintenanceCategory.CORRECTIVE, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(MaintenanceRecord::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByStatus_whenProvided() {
        Vehicle vehicle = persistVehicle("7555EEE");
        MaintenanceRecord match = persistMaintenance(vehicle, null, MaintenanceStatus.IN_PROGRESS);
        persistMaintenance(vehicle, null, MaintenanceStatus.SCHEDULED);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, null, null, null, MaintenanceStatus.IN_PROGRESS, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(MaintenanceRecord::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByTechnicianId_exactMatch_whenProvided() {
        Worker matchingTechnician = persistTechnician("77666666F");
        Worker otherTechnician = persistTechnician("77777777G");
        Vehicle vehicle = persistVehicle("7666FFF");
        MaintenanceRecord match = persistMaintenance(vehicle, matchingTechnician, MaintenanceStatus.SCHEDULED);
        persistMaintenance(vehicle, otherTechnician, MaintenanceStatus.SCHEDULED);
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, null, null, null, null, matchingTechnician.getId(), null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(MaintenanceRecord::getId).containsExactly(match.getId());
    }

    @Test
    void findAllJoinFetch_narrowsByCostRange_whenProvided() {
        Vehicle vehicle = persistVehicle("7777HHH");
        MaintenanceRecord match = persistMaintenanceWithCost(vehicle, new BigDecimal("100.00"), LocalDate.now());
        persistMaintenanceWithCost(vehicle, new BigDecimal("500.00"), LocalDate.now());
        entityManager.getEntityManager().clear();

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(
                null, null, null, null, null, null, null, new BigDecimal("50.00"), new BigDecimal("150.00"),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(MaintenanceRecord::getId).containsExactly(match.getId());
    }

    @Test
    void existsByVehicleIdAndStatus_returnsTrue_whenInProgressExists() {
        Vehicle vehicle = persistVehicle("3333CCC");
        persistMaintenance(vehicle, null, MaintenanceStatus.IN_PROGRESS);
        entityManager.getEntityManager().clear();

        assertThat(maintenanceRepository.existsByVehicleIdAndStatus(vehicle.getId(), MaintenanceStatus.IN_PROGRESS))
                .isTrue();
    }

    @Test
    void existsByVehicleIdAndStatus_returnsFalse_whenNoMaintenanceInThatStatus() {
        Vehicle vehicle = persistVehicle("4444DDD");
        persistMaintenance(vehicle, null, MaintenanceStatus.SCHEDULED);
        entityManager.getEntityManager().clear();

        assertThat(maintenanceRepository.existsByVehicleIdAndStatus(vehicle.getId(), MaintenanceStatus.IN_PROGRESS))
                .isFalse();
    }

    @Test
    void sumCostByWorkshopEntryDateBetween_sumsOnlyRecordsInRange() {
        Vehicle vehicle = persistVehicle("5555EEE");
        persistMaintenanceWithCost(vehicle, new BigDecimal("100.00"), LocalDate.now());
        persistMaintenanceWithCost(vehicle, new BigDecimal("50.00"), LocalDate.now());
        // Outside the queried range — must not be included in the sum.
        persistMaintenanceWithCost(vehicle, new BigDecimal("999.00"), LocalDate.now().minusMonths(2));
        entityManager.getEntityManager().clear();

        BigDecimal total = maintenanceRepository.sumCostByWorkshopEntryDateBetween(
                LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        assertThat(total).isEqualByComparingTo("150.00");
    }

    @Test
    void sumCostByWorkshopEntryDateBetween_returnsZero_whenNoRecordsInRange() {
        Vehicle vehicle = persistVehicle("6666FFF");
        persistMaintenanceWithCost(vehicle, new BigDecimal("100.00"), LocalDate.now().minusMonths(3));
        entityManager.getEntityManager().clear();

        BigDecimal total = maintenanceRepository.sumCostByWorkshopEntryDateBetween(
                LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private MaintenanceRecord persistMaintenanceWithCost(Vehicle vehicle, BigDecimal cost, LocalDate workshopEntryDate) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setType("Oil change");
        record.setCost(cost);
        record.setWorkshopEntryDate(workshopEntryDate);
        return entityManager.persistAndFlush(record);
    }

    private Worker persistTechnician(String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName("Test");
        worker.setLastName("Technician");
        worker.setWorkerRole(WorkerRole.TECHNICIAN);
        worker.setNationalId(nationalId);
        return entityManager.persistAndFlush(worker);
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

    private MaintenanceRecord persistMaintenance(Vehicle vehicle, Worker technician, MaintenanceStatus status) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setTechnician(technician);
        record.setType("Oil change");
        record.setStatus(status);
        return entityManager.persistAndFlush(record);
    }

    private MaintenanceRecord persistMaintenanceWithType(Vehicle vehicle, String type) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setType(type);
        record.setStatus(MaintenanceStatus.SCHEDULED);
        return entityManager.persistAndFlush(record);
    }

    private MaintenanceRecord persistMaintenanceWithCategory(Vehicle vehicle, MaintenanceCategory category) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setType("Oil change");
        record.setStatus(MaintenanceStatus.SCHEDULED);
        record.setCategory(category);
        return entityManager.persistAndFlush(record);
    }
}

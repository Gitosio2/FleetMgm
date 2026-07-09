package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
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

import java.time.Instant;

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

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(PageRequest.of(0, 20));

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

        Page<MaintenanceRecord> result = maintenanceRepository.findAllJoinFetch(PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
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
}

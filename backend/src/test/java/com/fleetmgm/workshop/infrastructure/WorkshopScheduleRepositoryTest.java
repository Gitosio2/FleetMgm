package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class WorkshopScheduleRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WorkshopScheduleRepository workshopScheduleRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllByScheduledDateBetween_returnsOnlyRecordsInRange_andInitializesAssociations() {
        Vehicle vehicle = persistVehicle("1111AAA");
        Worker technician = persistTechnician("11111111A");
        persistSchedule(vehicle, technician, LocalDate.now());
        persistSchedule(vehicle, technician, LocalDate.now().plusDays(20));
        entityManager.getEntityManager().clear();

        Page<WorkshopSchedule> result = workshopScheduleRepository.findAllByScheduledDateBetween(
                LocalDate.now(), LocalDate.now(), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        WorkshopSchedule fetched = result.getContent().get(0);
        assertThat(Hibernate.isInitialized(fetched.getVehicle())).isTrue();
        assertThat(Hibernate.isInitialized(fetched.getTechnician())).isTrue();
    }

    @Test
    void findAllByScheduledDateBetween_excludesRecordsOutsidePeriod() {
        Vehicle vehicle = persistVehicle("2222BBB");
        // Last day of the previous month — always outside [firstDayOfMonth, lastDayOfMonth] of
        // the current month regardless of which day of the month the suite runs on (a fixed
        // minusDays(N) offset can land back inside the current month depending on today's date).
        persistSchedule(vehicle, null, LocalDate.now().withDayOfMonth(1).minusDays(1));
        entityManager.getEntityManager().clear();

        Page<WorkshopSchedule> result = workshopScheduleRepository.findAllByScheduledDateBetween(
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllByScheduledDateBetween_excludesSoftDeleted() {
        Vehicle vehicle = persistVehicle("3333CCC");
        WorkshopSchedule schedule = persistSchedule(vehicle, null, LocalDate.now());
        schedule.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(schedule);
        entityManager.getEntityManager().clear();

        Page<WorkshopSchedule> result = workshopScheduleRepository.findAllByScheduledDateBetween(
                LocalDate.now(), LocalDate.now(), PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByMaintenanceRecordId_returnsLinkedSchedule() {
        Vehicle vehicle = persistVehicle("4444DDD");
        MaintenanceRecord maintenanceRecord = persistMaintenance(vehicle);
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setVehicle(vehicle);
        schedule.setMaintenanceRecord(maintenanceRecord);
        schedule.setScheduledDate(LocalDate.now());
        schedule.setType("Oil change");
        entityManager.persistAndFlush(schedule);
        entityManager.getEntityManager().clear();

        Optional<WorkshopSchedule> result = workshopScheduleRepository.findByMaintenanceRecordId(maintenanceRecord.getId());

        assertThat(result).isPresent();
    }

    @Test
    void findByMaintenanceRecordId_returnsEmpty_whenNoScheduleLinked() {
        Optional<WorkshopSchedule> result = workshopScheduleRepository.findByMaintenanceRecordId(UUID.randomUUID());

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

    private Worker persistTechnician(String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName("Test");
        worker.setLastName("Technician");
        worker.setWorkerRole(WorkerRole.TECHNICIAN);
        worker.setNationalId(nationalId);
        return entityManager.persistAndFlush(worker);
    }

    private MaintenanceRecord persistMaintenance(Vehicle vehicle) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setType("Oil change");
        return entityManager.persistAndFlush(record);
    }

    private WorkshopSchedule persistSchedule(Vehicle vehicle, Worker technician, LocalDate scheduledDate) {
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setVehicle(vehicle);
        schedule.setTechnician(technician);
        schedule.setScheduledDate(scheduledDate);
        schedule.setType("Oil change");
        return entityManager.persistAndFlush(schedule);
    }
}

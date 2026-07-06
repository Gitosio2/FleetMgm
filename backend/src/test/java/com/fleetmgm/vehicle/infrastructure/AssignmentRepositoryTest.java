package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class AssignmentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    AssignmentRepository assignmentRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findActiveByDriverId_returnsAssignment_whenActive() {
        Worker driver = persistDriver("11111111A");
        Vehicle vehicle = persistVehicle("1111AAA");
        User assignedBy = persistUser("admin1@example.com");
        assignmentRepository.saveAndFlush(buildAssignment(driver, vehicle, assignedBy, null));

        Optional<DriverVehicleAssignment> result = assignmentRepository.findActiveByDriverId(driver.getId());

        assertThat(result).isPresent();
    }

    @Test
    void findActiveByDriverId_returnsEmpty_whenEnded() {
        Worker driver = persistDriver("22222222B");
        Vehicle vehicle = persistVehicle("2222BBB");
        User assignedBy = persistUser("admin2@example.com");
        assignmentRepository.saveAndFlush(buildAssignment(driver, vehicle, assignedBy, LocalDate.now()));

        Optional<DriverVehicleAssignment> result = assignmentRepository.findActiveByDriverId(driver.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void save_throwsConstraintViolation_whenDriverHasTwoActiveAssignments() {
        Worker driver = persistDriver("33333333C");
        Vehicle vehicleOne = persistVehicle("3333CCC");
        Vehicle vehicleTwo = persistVehicle("4444DDD");
        User assignedBy = persistUser("admin3@example.com");
        assignmentRepository.saveAndFlush(buildAssignment(driver, vehicleOne, assignedBy, null));

        DriverVehicleAssignment second = buildAssignment(driver, vehicleTwo, assignedBy, null);

        assertThatThrownBy(() -> assignmentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Worker persistDriver(String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName("Test");
        worker.setLastName("Driver");
        worker.setWorkerRole(WorkerRole.DRIVER);
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

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setAppRole(AppRole.ADMIN);
        return entityManager.persistAndFlush(user);
    }

    private DriverVehicleAssignment buildAssignment(Worker driver, Vehicle vehicle, User assignedBy, LocalDate endDate) {
        DriverVehicleAssignment assignment = new DriverVehicleAssignment();
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStartDate(LocalDate.now());
        assignment.setEndDate(endDate);
        assignment.setAssignedByUser(assignedBy);
        return assignment;
    }
}

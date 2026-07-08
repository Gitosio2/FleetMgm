package com.fleetmgm.job.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class JobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    JobRepository jobRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllJoinFetch_initializesVehicleDriverAndClient_withoutFurtherQueries() {
        Worker driver = persistDriver("11111111A");
        Vehicle vehicle = persistVehicle("1111AAA");
        Job job = persistJob("Delivery", vehicle, driver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.findAllJoinFetch(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        Job fetched = result.getContent().get(0);
        assertThat(fetched.getId()).isEqualTo(job.getId());
        assertThat(Hibernate.isInitialized(fetched.getVehicle())).isTrue();
        assertThat(Hibernate.isInitialized(fetched.getAssignedDriver())).isTrue();
    }

    @Test
    void findByAssignedDriverIdAndStatusIn_returnsOnlyMatchingActiveJobsForDriver() {
        Worker driver = persistDriver("22222222B");
        Worker otherDriver = persistDriver("33333333C");
        Vehicle vehicle = persistVehicle("2222BBB");
        persistJob("Active job", vehicle, driver, JobStatus.PENDING);
        persistJob("In progress job", vehicle, driver, JobStatus.IN_PROGRESS);
        persistJob("Completed job", vehicle, driver, JobStatus.COMPLETED);
        persistJob("Other driver job", vehicle, otherDriver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.findByAssignedDriverIdAndStatusIn(
                driver.getId(), List.of(JobStatus.PENDING, JobStatus.IN_PROGRESS), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Job::getTitle)
                .containsExactlyInAnyOrder("Active job", "In progress job");
    }

    @Test
    void findByAssignedDriverIdAndStatusIn_excludesSoftDeletedJobs() {
        Worker driver = persistDriver("44444444D");
        Vehicle vehicle = persistVehicle("3333CCC");
        Job deleted = persistJob("Deleted job", vehicle, driver, JobStatus.PENDING);
        deleted.setDeletedAt(Instant.now());
        entityManager.persistAndFlush(deleted);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.findByAssignedDriverIdAndStatusIn(
                driver.getId(), List.of(JobStatus.PENDING), PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
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

    private Job persistJob(String title, Vehicle vehicle, Worker driver, JobStatus status) {
        Job job = new Job();
        job.setTitle(title);
        job.setVehicle(vehicle);
        job.setAssignedDriver(driver);
        job.setStatus(status);
        job.setOriginLocation("Origin");
        job.setDestinationLocation("Destination");
        return entityManager.persistAndFlush(job);
    }
}

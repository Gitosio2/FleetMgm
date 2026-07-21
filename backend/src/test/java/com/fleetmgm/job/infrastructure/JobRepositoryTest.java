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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void findAllJoinFetch_ordersNotStartedFirst_thenMostRecentlyStartedFirst() {
        Worker driver = persistDriver("55555555E");
        Vehicle vehicle = persistVehicle("4444DDD");
        Job notStarted = persistJobWithActualStart("Not started", vehicle, driver, null);
        Job oldestStart = persistJobWithActualStart(
                "Oldest start", vehicle, driver, Instant.parse("2026-01-01T00:00:00Z"));
        Job newestStart = persistJobWithActualStart(
                "Newest start", vehicle, driver, Instant.parse("2026-01-03T00:00:00Z"));
        Job middleStart = persistJobWithActualStart(
                "Middle start", vehicle, driver, Instant.parse("2026-01-02T00:00:00Z"));
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.findAllJoinFetch(PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(
                notStarted.getId(), newestStart.getId(), middleStart.getId(), oldestStart.getId());
    }

    @Test
    void findByAssignedDriverIdAndStatusIn_ordersNotStartedFirst_thenMostRecentlyStartedFirst() {
        Worker driver = persistDriver("66666666F");
        Vehicle vehicle = persistVehicle("5555EEE");
        Job notStarted = persistJobWithActualStart("Not started", vehicle, driver, null);
        Job oldestStart = persistJobWithActualStart(
                "Oldest start", vehicle, driver, Instant.parse("2026-01-01T00:00:00Z"));
        Job newestStart = persistJobWithActualStart(
                "Newest start", vehicle, driver, Instant.parse("2026-01-03T00:00:00Z"));
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.findByAssignedDriverIdAndStatusIn(
                driver.getId(), List.of(JobStatus.PENDING), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(
                notStarted.getId(), newestStart.getId(), oldestStart.getId());
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

    // --- search ---

    @Test
    void search_narrowsByTitle_partialMatch_caseInsensitive_whenProvided() {
        Worker driver = persistDriver("71111111A");
        Vehicle vehicle = persistVehicle("7111AAA");
        Job match = persistJob("Urgent delivery", vehicle, driver, JobStatus.PENDING);
        persistJob("Weekly round", vehicle, driver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                "urgent", null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByOriginAndDestination_partialMatch_caseInsensitive_whenProvided() {
        Worker driver = persistDriver("72222222B");
        Vehicle vehicle = persistVehicle("7222BBB");
        Job match = persistJobWithRoute("Job A", vehicle, driver, "Central Warehouse", "North Zone");
        persistJobWithRoute("Job B", vehicle, driver, "South Depot", "East Zone");
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, "central", "north", null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByVehicleId_exactMatch_whenProvided() {
        Worker driver = persistDriver("73333333C");
        Vehicle matchingVehicle = persistVehicle("7333CCC", "Toyota", "Hilux");
        Vehicle otherVehicle = persistVehicle("7444DDD", "Ford", "Ranger");
        Job match = persistJob("Job A", matchingVehicle, driver, JobStatus.PENDING);
        persistJob("Job B", otherVehicle, driver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, matchingVehicle.getId(), null, null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByAssignedDriverId_exactMatch_whenProvided() {
        Worker matchingDriver = persistDriver("Juan", "García", "74444444D");
        Worker otherDriver = persistDriver("Ana", "López", "74444444E");
        Vehicle vehicle = persistVehicle("7555EEE");
        Job match = persistJob("Job A", vehicle, matchingDriver, JobStatus.PENDING);
        persistJob("Job B", vehicle, otherDriver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, matchingDriver.getId(), null, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_excludesJobsWithNoAssignedDriver_whenAssignedDriverIdFilterProvided() {
        Worker driver = persistDriver("74555555F");
        Vehicle vehicle = persistVehicle("7666FFF");
        persistJob("Unassigned job", vehicle, null, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, driver.getId(), null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void search_narrowsByStatus_whenProvided() {
        Worker driver = persistDriver("76666666G");
        Vehicle vehicle = persistVehicle("7777GGG");
        Job match = persistJob("Job A", vehicle, driver, JobStatus.IN_PROGRESS);
        persistJob("Job B", vehicle, driver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, null, JobStatus.IN_PROGRESS, null, null, null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByActualStartRange_whenProvided() {
        Worker driver = persistDriver("77777777H");
        Vehicle vehicle = persistVehicle("7888HHH");
        Job match = persistJobWithActualStart("Job A", vehicle, driver, Instant.parse("2026-01-15T00:00:00Z"));
        persistJobWithActualStart("Job B", vehicle, driver, Instant.parse("2026-03-01T00:00:00Z"));
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T23:59:59Z"), null, null,
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByActualEndRange_whenProvided() {
        Worker driver = persistDriver("78888888I");
        Vehicle vehicle = persistVehicle("7999III");
        Job match = persistJobWithActualEnd("Job A", vehicle, driver, Instant.parse("2026-01-15T00:00:00Z"));
        persistJobWithActualEnd("Job B", vehicle, driver, Instant.parse("2026-03-01T00:00:00Z"));
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T23:59:59Z"),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(match.getId());
    }

    @Test
    void search_returnsAll_whenNoFiltersProvided() {
        Worker driver = persistDriver("79999999J");
        Vehicle vehicle = persistVehicle("7000JJJ");
        persistJob("Job A", vehicle, driver, JobStatus.PENDING);
        persistJob("Job B", vehicle, driver, JobStatus.IN_PROGRESS);
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void search_ordersNotStartedFirst_thenMostRecentlyStartedFirst() {
        Worker driver = persistDriver("70000000K");
        Vehicle vehicle = persistVehicle("7100KKK");
        Job notStarted = persistJobWithActualStart("Not started", vehicle, driver, null);
        Job oldestStart = persistJobWithActualStart(
                "Oldest start", vehicle, driver, Instant.parse("2026-01-01T00:00:00Z"));
        Job newestStart = persistJobWithActualStart(
                "Newest start", vehicle, driver, Instant.parse("2026-01-03T00:00:00Z"));
        entityManager.getEntityManager().clear();

        Page<Job> result = jobRepository.search(
                null, null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Job::getId).containsExactly(
                notStarted.getId(), newestStart.getId(), oldestStart.getId());
    }

    // --- optimistic locking ---

    // Simulates two concurrent requests (e.g. a double-click on "complete") that both load the job
    // before either commits: each gets its own copy at version 0, and the second save must be
    // rejected instead of silently overwriting the first transaction's outcome (which would double-fire
    // JobCompletedEvent — see JobService.complete()).
    @Test
    void save_throwsOptimisticLockingFailure_whenTwoConcurrentlyLoadedCopiesAreBothSaved() {
        Worker driver = persistDriver("81111111A");
        Vehicle vehicle = persistVehicle("8111AAA");
        Job job = persistJob("Delivery", vehicle, driver, JobStatus.PENDING);
        entityManager.getEntityManager().clear();

        // Each findById + detach mimics a separate request loading its own snapshot before either
        // commits (a single persistence context would return the same identity-mapped instance for
        // both calls, masking the staleness this test exists to catch).
        Job firstCopy = jobRepository.findById(job.getId()).orElseThrow();
        entityManager.getEntityManager().detach(firstCopy);
        Job secondCopy = jobRepository.findById(job.getId()).orElseThrow();
        entityManager.getEntityManager().detach(secondCopy);

        firstCopy.setStatus(JobStatus.IN_PROGRESS);
        jobRepository.saveAndFlush(firstCopy);

        secondCopy.setStatus(JobStatus.CANCELLED);
        assertThatThrownBy(() -> jobRepository.saveAndFlush(secondCopy))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private Worker persistDriver(String nationalId) {
        return persistDriver("Test", "Driver", nationalId);
    }

    private Worker persistDriver(String firstName, String lastName, String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName(firstName);
        worker.setLastName(lastName);
        worker.setWorkerRole(WorkerRole.DRIVER);
        worker.setNationalId(nationalId);
        return entityManager.persistAndFlush(worker);
    }

    private Vehicle persistVehicle(String licensePlate) {
        return persistVehicle(licensePlate, "Toyota", "Hilux");
    }

    private Vehicle persistVehicle(String licensePlate, String make, String model) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleCategory(VehicleCategory.LIGHT_VEHICLE);
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setMake(make);
        vehicle.setModel(model);
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

    private Job persistJobWithRoute(String title, Vehicle vehicle, Worker driver,
            String originLocation, String destinationLocation) {
        Job job = new Job();
        job.setTitle(title);
        job.setVehicle(vehicle);
        job.setAssignedDriver(driver);
        job.setStatus(JobStatus.PENDING);
        job.setOriginLocation(originLocation);
        job.setDestinationLocation(destinationLocation);
        return entityManager.persistAndFlush(job);
    }

    private Job persistJobWithActualStart(String title, Vehicle vehicle, Worker driver, Instant actualStart) {
        Job job = new Job();
        job.setTitle(title);
        job.setVehicle(vehicle);
        job.setAssignedDriver(driver);
        job.setStatus(JobStatus.PENDING);
        job.setOriginLocation("Origin");
        job.setDestinationLocation("Destination");
        job.setActualStart(actualStart);
        return entityManager.persistAndFlush(job);
    }

    private Job persistJobWithActualEnd(String title, Vehicle vehicle, Worker driver, Instant actualEnd) {
        Job job = new Job();
        job.setTitle(title);
        job.setVehicle(vehicle);
        job.setAssignedDriver(driver);
        job.setStatus(JobStatus.COMPLETED);
        job.setOriginLocation("Origin");
        job.setDestinationLocation("Destination");
        job.setActualStart(actualEnd.minusSeconds(3600));
        job.setActualEnd(actualEnd);
        return entityManager.persistAndFlush(job);
    }
}

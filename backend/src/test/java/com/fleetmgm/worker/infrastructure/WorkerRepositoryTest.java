package com.fleetmgm.worker.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
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
class WorkerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WorkerRepository workerRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAll_excludesSoftDeleted() {
        Worker active = buildWorker("11111111A");
        Worker deleted = buildWorker("22222222B");
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();
        entityManager.clear();

        List<Worker> results = workerRepository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNationalId()).isEqualTo("11111111A");
    }

    @Test
    void existsByNationalId_returnsTrue_whenExists() {
        entityManager.persistAndFlush(buildWorker("12345678A"));

        assertThat(workerRepository.existsByNationalId("12345678A")).isTrue();
        assertThat(workerRepository.existsByNationalId("ZZZZ")).isFalse();
    }

    @Test
    void search_narrowsByName_partialMatch_caseInsensitive_acrossFirstAndLastName_whenProvided() {
        Worker match = entityManager.persistAndFlush(
                buildWorker("Juan", "García", WorkerRole.DRIVER, "31111111A"));
        entityManager.persistAndFlush(buildWorker("Ana", "López", WorkerRole.DRIVER, "31111111B"));
        entityManager.clear();

        Page<Worker> result = workerRepository.search("juan garc", null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Worker::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByNationalId_partialMatch_caseInsensitive_whenProvided() {
        Worker match = entityManager.persistAndFlush(
                buildWorker("Juan", "García", WorkerRole.DRIVER, "32222222A"));
        entityManager.persistAndFlush(buildWorker("Ana", "López", WorkerRole.DRIVER, "32222222B"));
        entityManager.clear();

        Page<Worker> result = workerRepository.search(null, "32222222a", null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Worker::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByWorkerRole_whenProvided() {
        Worker match = entityManager.persistAndFlush(
                buildWorker("Juan", "García", WorkerRole.TECHNICIAN, "33333333A"));
        entityManager.persistAndFlush(buildWorker("Ana", "López", WorkerRole.DRIVER, "33333333B"));
        entityManager.clear();

        Page<Worker> result = workerRepository.search(null, null, WorkerRole.TECHNICIAN, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Worker::getId).containsExactly(match.getId());
    }

    @Test
    void search_combinesAllFilters_whenAllProvided() {
        Worker match = entityManager.persistAndFlush(
                buildWorker("Juan", "García", WorkerRole.BOTH, "34444444A"));
        entityManager.persistAndFlush(buildWorker("Juan", "García", WorkerRole.DRIVER, "34444444B"));
        entityManager.persistAndFlush(buildWorker("Ana", "López", WorkerRole.BOTH, "34444444C"));
        entityManager.clear();

        Page<Worker> result = workerRepository.search("juan", "34444444a", WorkerRole.BOTH, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Worker::getId).containsExactly(match.getId());
    }

    @Test
    void search_returnsAll_whenNoFiltersProvided() {
        entityManager.persistAndFlush(buildWorker("Juan", "García", WorkerRole.DRIVER, "35555555A"));
        entityManager.persistAndFlush(buildWorker("Ana", "López", WorkerRole.TECHNICIAN, "35555555B"));
        entityManager.clear();

        Page<Worker> result = workerRepository.search(null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    private Worker buildWorker(String nationalId) {
        return buildWorker("Test", "Worker", WorkerRole.DRIVER, nationalId);
    }

    private Worker buildWorker(String firstName, String lastName, WorkerRole role, String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName(firstName);
        worker.setLastName(lastName);
        worker.setWorkerRole(role);
        worker.setNationalId(nationalId);
        return worker;
    }
}

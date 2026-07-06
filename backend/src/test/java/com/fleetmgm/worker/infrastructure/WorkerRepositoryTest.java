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

    private Worker buildWorker(String nationalId) {
        Worker worker = new Worker();
        worker.setFirstName("Test");
        worker.setLastName("Worker");
        worker.setWorkerRole(WorkerRole.DRIVER);
        worker.setNationalId(nationalId);
        return worker;
    }
}

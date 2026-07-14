package com.fleetmgm.shared.infrastructure;

import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

// Regression coverage for a real Hito 46 bug: findAllFiltered's "(:param IS NULL OR ...)" idiom
// with all-null filters passed PostgreSQL a bind parameter PostgreSQL could not type-infer on its
// own (bare "? IS NULL" with no column/function context), failing every request with either
// "function lower(bytea) does not exist" (performedByEmail, once wrapped in LOWER/CONCAT) or
// "could not determine data type of parameter $N" (entityType/action/from/to). AuditLogController
// only had a @WebMvcTest with a mocked service, which never touched real SQL — this class is the
// first test in the codebase to actually run findAllFiltered against real PostgreSQL.
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class AuditLogRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void findAllFiltered_returnsAll_whenEveryFilterIsNull() {
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", Instant.now());
        persistLog("Job", AuditAction.UPDATE, "gerente@fleetmgm.demo", Instant.now());
        entityManager.getEntityManager().clear();

        Page<AuditLog> result = auditLogRepository.findAllFiltered(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllFiltered_narrowsByEntityType_whenProvided() {
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", Instant.now());
        persistLog("Job", AuditAction.CREATE, "admin@fleetmgm.demo", Instant.now());
        entityManager.getEntityManager().clear();

        Page<AuditLog> result = auditLogRepository.findAllFiltered(
                "Vehicle", null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEntityType()).isEqualTo("Vehicle");
    }

    @Test
    void findAllFiltered_narrowsByAction_whenProvided() {
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", Instant.now());
        persistLog("Vehicle", AuditAction.DELETE, "admin@fleetmgm.demo", Instant.now());
        entityManager.getEntityManager().clear();

        Page<AuditLog> result = auditLogRepository.findAllFiltered(
                null, AuditAction.DELETE, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo(AuditAction.DELETE);
    }

    @Test
    void findAllFiltered_narrowsByDateRange_whenProvided() {
        Instant now = Instant.now();
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", now.minus(10, ChronoUnit.DAYS));
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", now);
        entityManager.getEntityManager().clear();

        Page<AuditLog> result = auditLogRepository.findAllFiltered(
                null, null, now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllFiltered_narrowsByPerformedByEmail_caseInsensitiveSubstring() {
        persistLog("Vehicle", AuditAction.CREATE, "admin@fleetmgm.demo", Instant.now());
        persistLog("Vehicle", AuditAction.CREATE, "conductor1@fleetmgm.demo", Instant.now());
        entityManager.getEntityManager().clear();

        Page<AuditLog> result = auditLogRepository.findAllFiltered(
                null, null, null, null, "ADMIN", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPerformedByEmail()).isEqualTo("admin@fleetmgm.demo");
    }

    private AuditLog persistLog(String entityType, AuditAction action, String performedByEmail, Instant performedAt) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId("00000000-0000-0000-0000-000000000001");
        log.setAction(action);
        log.setPerformedByEmail(performedByEmail);
        log.setPerformedAt(performedAt);
        return entityManager.persistAndFlush(log);
    }
}

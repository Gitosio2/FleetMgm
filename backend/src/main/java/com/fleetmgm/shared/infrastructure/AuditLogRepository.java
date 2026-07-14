package com.fleetmgm.shared.infrastructure;

import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // All filters are optional — the "(:param IS NULL OR ...)" JPQL idiom (same as
    // SupplierInvoiceRepository.findAllJoinFetch) keeps the query fully parameterized, satisfying
    // the dynamic-query SQL injection rule while still supporting Pageable. performedByEmail is a
    // case-insensitive substring match (LIKE on a bind parameter, not concatenated SQL) since the
    // admin types a partial email rather than picking from a user list.
    // Every "(:param IS NULL OR ...)" check below casts the bare occurrence with CAST(:param AS
    // string) — not cosmetic. Hibernate emits each textual occurrence of a named parameter as its
    // OWN JDBC bind position (confirmed via the generated SQL), and a parameter that appears ONLY
    // in a bare "? IS NULL" comparison gives PostgreSQL no column/function context to infer a type
    // from. Left uncast, every request — even with zero filters — failed 500 with either
    // "function lower(bytea) does not exist" (the email param, once wrapped in LOWER/CONCAT gave
    // Postgres a conflicting type guess) or "could not determine data type of parameter $N" (the
    // other params, entirely untyped). Casting to `string` is safe here regardless of the
    // parameter's real Java type (String/enum/Instant): IS NULL doesn't care about the compared
    // type, so CAST(x AS string) IS NULL is equivalent to x IS NULL for every input.
    @Query("SELECT a FROM AuditLog a WHERE (CAST(:entityType AS string) IS NULL OR a.entityType = :entityType) "
            + "AND (CAST(:action AS string) IS NULL OR a.action = :action) "
            + "AND (CAST(:from AS string) IS NULL OR a.performedAt >= :from) "
            + "AND (CAST(:to AS string) IS NULL OR a.performedAt <= :to) "
            + "AND (CAST(:performedByEmail AS string) IS NULL OR LOWER(a.performedByEmail) "
            + "LIKE LOWER(CONCAT('%', CAST(:performedByEmail AS string), '%')))")
    Page<AuditLog> findAllFiltered(
            @Param("entityType") String entityType, @Param("action") AuditAction action,
            @Param("from") Instant from, @Param("to") Instant to,
            @Param("performedByEmail") String performedByEmail, Pageable pageable);

    // Distinct performers already present in audit_logs, for the frontend's user filter dropdown.
    // Scoped to this feature on purpose — not a query against the full users table (see
    // AuditLogPerformerResponse). performedByEmail can be null in theory (see AuditLog entity), so
    // null rows are excluded to avoid a blank option; ordered by email for a stable dropdown.
    @Query("SELECT DISTINCT a.performedByEmail FROM AuditLog a WHERE a.performedByEmail IS NOT NULL "
            + "ORDER BY a.performedByEmail ASC")
    List<String> findDistinctPerformerEmails();
}

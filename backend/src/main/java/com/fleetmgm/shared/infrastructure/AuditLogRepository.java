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
    @Query("SELECT a FROM AuditLog a WHERE (:entityType IS NULL OR a.entityType = :entityType) "
            + "AND (:action IS NULL OR a.action = :action) "
            + "AND (:from IS NULL OR a.performedAt >= :from) "
            + "AND (:to IS NULL OR a.performedAt <= :to) "
            + "AND (:performedByEmail IS NULL OR LOWER(a.performedByEmail) LIKE LOWER(CONCAT('%', :performedByEmail, '%')))")
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

package com.fleetmgm.shared.infrastructure;

import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // All four filters are optional — the "(:param IS NULL OR ...)" JPQL idiom (same as
    // SupplierInvoiceRepository.findAllJoinFetch) keeps the query fully parameterized, satisfying
    // the dynamic-query SQL injection rule while still supporting Pageable.
    @Query("SELECT a FROM AuditLog a WHERE (:entityType IS NULL OR a.entityType = :entityType) "
            + "AND (:action IS NULL OR a.action = :action) "
            + "AND (:from IS NULL OR a.performedAt >= :from) "
            + "AND (:to IS NULL OR a.performedAt <= :to)")
    Page<AuditLog> findAllFiltered(
            @Param("entityType") String entityType, @Param("action") AuditAction action,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}

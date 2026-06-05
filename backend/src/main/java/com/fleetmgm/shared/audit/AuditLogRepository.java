package com.fleetmgm.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByPerformedByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByPerformedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

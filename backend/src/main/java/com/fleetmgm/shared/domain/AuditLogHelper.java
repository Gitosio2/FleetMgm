package com.fleetmgm.shared.domain;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditLogHelper {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogHelper(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(String entityType, String entityId, AuditAction action) {
        log(entityType, entityId, action, null);
    }

    public void log(String entityType, String entityId, AuditAction action, String details) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setPerformedByEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> auditLog.setPerformedByUserId(user.getId()));
        auditLog.setPerformedAt(Instant.now());
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }
}

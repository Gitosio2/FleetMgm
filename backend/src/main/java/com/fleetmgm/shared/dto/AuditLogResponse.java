package com.fleetmgm.shared.dto;

import com.fleetmgm.shared.domain.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityType,
        String entityId,
        AuditAction action,
        UUID performedByUserId,
        String performedByEmail,
        Instant performedAt,
        String ipAddress,
        String oldValues,
        String newValues,
        String details
) {}

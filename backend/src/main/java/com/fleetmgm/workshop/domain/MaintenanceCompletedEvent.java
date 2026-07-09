package com.fleetmgm.workshop.domain;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceCompletedEvent(
        UUID maintenanceId,
        UUID vehicleId,
        Instant completedAt
) {}

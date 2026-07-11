package com.fleetmgm.workshop.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceScheduledEvent(
        UUID maintenanceId,
        UUID vehicleId,
        UUID technicianId,
        LocalDate scheduledDate,
        String type,
        Instant scheduledAt
) {}

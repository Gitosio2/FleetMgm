package com.fleetmgm.workshop.domain;

import java.time.Instant;
import java.util.UUID;

public record MaintenanceCancelledEvent(
        UUID maintenanceId,
        UUID vehicleId,
        Instant cancelledAt
) {}

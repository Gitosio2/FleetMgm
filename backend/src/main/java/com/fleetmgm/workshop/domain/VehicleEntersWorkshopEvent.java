package com.fleetmgm.workshop.domain;

import java.time.Instant;
import java.util.UUID;

public record VehicleEntersWorkshopEvent(
        UUID maintenanceId,
        UUID vehicleId,
        Instant enteredAt
) {}

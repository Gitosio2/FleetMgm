package com.fleetmgm.vehicle.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID driverId,
        String driverName,
        UUID vehicleId,
        String vehicleLicensePlate,
        LocalDate startDate,
        LocalDate endDate,
        UUID assignedByUserId,
        String notes,
        Instant createdAt,
        boolean active
) {}

package com.fleetmgm.vehicle.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID driverId,
        @NotNull UUID vehicleId,
        @NotNull LocalDate startDate,
        String notes
) {}

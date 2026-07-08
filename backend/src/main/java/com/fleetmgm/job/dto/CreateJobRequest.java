package com.fleetmgm.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID vehicleId,
        UUID assignedDriverId,
        UUID clientId,
        @NotBlank String title,
        String description,
        @NotBlank String originLocation,
        @NotBlank String destinationLocation,
        String notes,
        Instant scheduledStart,
        Instant scheduledEnd
) {}

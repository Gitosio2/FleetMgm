package com.fleetmgm.job.dto;

import com.fleetmgm.job.domain.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String description,
        UUID vehicleId,
        String vehicleLicensePlate,
        UUID assignedDriverId,
        String assignedDriverName,
        UUID clientId,
        String clientName,
        JobStatus status,
        String originLocation,
        String destinationLocation,
        String notes,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        Long startUsageValue,
        Long endUsageValue,
        Instant createdAt
) {}

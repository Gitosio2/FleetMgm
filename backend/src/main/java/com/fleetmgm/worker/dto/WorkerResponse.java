package com.fleetmgm.worker.dto;

import com.fleetmgm.worker.domain.WorkerRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkerResponse(
        UUID id,
        String firstName,
        String lastName,
        String fullName,
        WorkerRole workerRole,
        String nationalId,
        String phone,
        String licenseType,
        LocalDate licenseExpiry,
        UUID userId,
        Instant createdAt
) {}

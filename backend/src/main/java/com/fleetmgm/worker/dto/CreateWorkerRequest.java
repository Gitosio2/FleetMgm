package com.fleetmgm.worker.dto;

import com.fleetmgm.worker.domain.WorkerRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateWorkerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull WorkerRole workerRole,
        @NotBlank String nationalId,
        String phone,
        String licenseType,
        LocalDate licenseExpiry,
        UUID userId
) {}

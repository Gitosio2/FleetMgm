package com.fleetmgm.worker.dto;

import com.fleetmgm.worker.domain.WorkerRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateWorkerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull WorkerRole workerRole,
        String phone,
        String licenseType,
        LocalDate licenseExpiry
) {}

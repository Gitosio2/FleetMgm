package com.fleetmgm.workshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMaintenanceRequest(
        @NotNull UUID vehicleId,
        @NotBlank String type,
        String description,
        UUID technicianId
) {}

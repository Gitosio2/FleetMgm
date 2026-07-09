package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.MaintenanceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMaintenanceRequest(
        @NotNull UUID vehicleId,
        @NotBlank String type,
        String description,
        UUID technicianId,
        @NotNull MaintenanceCategory category
) {}

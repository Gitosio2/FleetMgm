package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.MaintenanceCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateMaintenanceRequest(
        @NotNull UUID vehicleId,
        @NotBlank String type,
        String description,
        UUID technicianId,
        @NotNull MaintenanceCategory category,
        @DecimalMin(value = "0") BigDecimal cost
) {}

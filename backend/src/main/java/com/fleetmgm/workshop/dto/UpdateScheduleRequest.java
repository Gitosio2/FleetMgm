package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.SchedulePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateScheduleRequest(
        @NotNull UUID vehicleId,
        UUID technicianId,
        UUID maintenanceRecordId,
        @NotNull LocalDate scheduledDate,
        @NotBlank String type,
        @NotNull SchedulePriority priority,
        String notes
) {}

package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.SchedulePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateScheduleRequest(
        @NotNull UUID vehicleId,
        UUID technicianId,
        UUID maintenanceRecordId,
        @NotNull LocalDate scheduledDate,
        @NotBlank String type,
        SchedulePriority priority,
        String notes
) {}

package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.SchedulePriority;
import com.fleetmgm.workshop.domain.WorkshopStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleMake,
        String vehicleModel,
        UUID technicianId,
        String technicianName,
        UUID maintenanceRecordId,
        MaintenanceCategory maintenanceCategory,
        LocalDate scheduledDate,
        String type,
        SchedulePriority priority,
        WorkshopStatus status,
        String notes,
        Instant createdAt,
        LocalTime scheduledStartTime,
        LocalTime scheduledEndTime
) {}

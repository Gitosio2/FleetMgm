package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record MaintenanceResponse(
        UUID id,
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleMake,
        String vehicleModel,
        String type,
        String description,
        Long usageAtService,
        BigDecimal cost,
        LocalDate workshopEntryDate,
        LocalDate workshopExitDate,
        UUID technicianId,
        String technicianName,
        UUID invoiceId,
        MaintenanceStatus status,
        MaintenanceCategory category,
        Instant createdAt,
        LocalTime workshopEntryTime,
        LocalTime workshopExitTime
) {}

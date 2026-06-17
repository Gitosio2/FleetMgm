package com.fleetmgm.vehicle.dto;

import com.fleetmgm.vehicle.domain.AcquisitionType;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        VehicleCategory vehicleCategory,
        UsageMeasure usageMeasure,
        String make,
        String model,
        Integer year,
        String licensePlate,
        String heavySubtype,
        String vin,
        String color,
        VehicleStatus status,
        Long currentKm,
        Long currentHours,
        AcquisitionType acquisitionType,
        LocalDate acquisitionDate,
        BigDecimal purchasePrice,
        Integer amortizationYears,
        BigDecimal monthlyFee,
        LocalDate contractEndDate,
        Instant createdAt
) {}

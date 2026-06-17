package com.fleetmgm.vehicle.dto;

import com.fleetmgm.vehicle.domain.AcquisitionType;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVehicleRequest(
        @NotNull VehicleCategory vehicleCategory,
        @NotNull UsageMeasure usageMeasure,
        @NotBlank String make,
        @NotBlank String model,
        @NotNull @Min(1900) Integer year,
        String licensePlate,
        String heavySubtype,
        String vin,
        String color,
        AcquisitionType acquisitionType,
        LocalDate acquisitionDate,
        BigDecimal purchasePrice,
        Integer amortizationYears,
        BigDecimal monthlyFee,
        LocalDate contractEndDate,
        Long currentKm,
        Long currentHours
) {}

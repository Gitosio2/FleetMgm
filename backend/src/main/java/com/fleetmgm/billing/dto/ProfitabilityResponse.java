package com.fleetmgm.billing.dto;

import com.fleetmgm.vehicle.domain.UsageMeasure;

import java.math.BigDecimal;
import java.util.UUID;

// costPerUsageUnit/profitPerUsageUnit/usageMeasure are only populated by getByVehicleId (single
// vehicle detail panel, VehicleProfitabilityPanel's Totales grid) — never by list() (fleet-wide
// paged table), which would require an extra usage-log query per row (N+1). See
// ProfitabilityService.toResponse overloads.
public record ProfitabilityResponse(
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleMake,
        String vehicleModel,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal margin,
        BigDecimal costPerUsageUnit,
        BigDecimal profitPerUsageUnit,
        UsageMeasure usageMeasure
) {}

package com.fleetmgm.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProfitabilityResponse(
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleMake,
        String vehicleModel,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal margin
) {}

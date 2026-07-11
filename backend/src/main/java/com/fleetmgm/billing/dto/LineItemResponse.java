package com.fleetmgm.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LineItemResponse(
        UUID id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        UUID linkedJobId,
        UUID linkedMaintenanceId
) {}

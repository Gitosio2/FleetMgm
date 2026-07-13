package com.fleetmgm.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleRevenueLineItemResponse(
        String invoiceNumber,
        LocalDate issueDate,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}

package com.fleetmgm.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateInvoiceRequest(
        @NotNull UUID clientId,
        LocalDate dueDate,
        String notes,
        // null means "leave the invoice's current tax rate unchanged" — different semantics from
        // CreateInvoiceRequest.taxRate(). 0 is a legally real reduced/bonified rate, so this must
        // not use @Positive.
        @DecimalMin(value = "0") BigDecimal taxRate
) {}

package com.fleetmgm.billing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateInvoiceRequest(
        @NotNull UUID clientId,
        LocalDate dueDate,
        String notes
) {}

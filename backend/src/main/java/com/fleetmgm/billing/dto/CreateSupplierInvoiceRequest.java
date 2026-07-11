package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateSupplierInvoiceRequest(
        @NotBlank String supplierName,
        String supplierInvoiceNumber,
        @NotNull ExpenseCategory category,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        UUID vehicleId,
        // Directly entered as it appears on the received invoice — not computed from line items,
        // unlike Invoice.subtotal/taxAmount/total (no issue()-equivalent step exists here). 0 is a
        // legally valid amount (e.g. a credit-note-like adjustment), so this must not use @Positive.
        @NotNull @DecimalMin(value = "0") BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0") BigDecimal taxAmount,
        @NotNull @DecimalMin(value = "0") BigDecimal total,
        String notes,
        String documentPath
) {}

package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateSupplierInvoiceRequest(
        @NotBlank String supplierName,
        String supplierInvoiceNumber,
        @NotNull ExpenseCategory category,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        UUID vehicleId,
        // Same rationale as CreateSupplierInvoiceRequest — directly provided, correctable by the
        // caller, never derived. 0 is a legally valid amount, so this must not use @Positive.
        @NotNull @DecimalMin(value = "0") BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0") BigDecimal taxAmount,
        @NotNull @DecimalMin(value = "0") BigDecimal total,
        String notes,
        String documentPath
) {}

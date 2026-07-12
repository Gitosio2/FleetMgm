package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierInvoiceResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierInvoiceNumber,
        ExpenseCategory category,
        LocalDate invoiceDate,
        LocalDate dueDate,
        LocalDate paymentDate,
        SupplierInvoiceStatus status,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        UUID vehicleId,
        String vehicleLicensePlate,
        String vehicleMake,
        String vehicleModel,
        String notes,
        String documentPath,
        Instant createdAt,
        List<SupplierLineItemResponse> lineItems
) {}

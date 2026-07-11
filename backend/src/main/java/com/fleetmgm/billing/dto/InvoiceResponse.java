package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID clientId,
        String clientName,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate paymentDate,
        BigDecimal taxRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        String notes,
        Instant createdAt
) {}

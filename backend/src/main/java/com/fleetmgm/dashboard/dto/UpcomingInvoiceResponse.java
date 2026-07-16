package com.fleetmgm.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpcomingInvoiceResponse(
        UUID id,
        String number,
        UUID counterpartyId,
        String counterparty,
        BigDecimal amount,
        LocalDate dueDate,
        boolean overdue
) {}

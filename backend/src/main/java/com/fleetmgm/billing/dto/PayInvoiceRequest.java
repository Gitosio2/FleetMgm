package com.fleetmgm.billing.dto;

import java.time.LocalDate;

public record PayInvoiceRequest(
        LocalDate paymentDate
) {}

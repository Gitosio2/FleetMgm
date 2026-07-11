package com.fleetmgm.job.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// clientId/price/title are denormalized onto the event (same pattern already used for
// vehicleId/endUsageValue) so the billing consumer (InvoiceJobCompletionListener) doesn't need
// to re-fetch the Job to build an auto-generated invoice line item.
public record JobCompletedEvent(
        UUID jobId,
        UUID vehicleId,
        UUID clientId,
        BigDecimal price,
        String title,
        Long endUsageValue,
        Instant completedAt
) {}

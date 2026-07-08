package com.fleetmgm.job.domain;

import java.time.Instant;
import java.util.UUID;

public record JobCompletedEvent(
        UUID jobId,
        UUID vehicleId,
        Long endUsageValue,
        Instant completedAt
) {}

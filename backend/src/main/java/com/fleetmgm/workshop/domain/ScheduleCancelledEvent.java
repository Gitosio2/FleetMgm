package com.fleetmgm.workshop.domain;

import java.time.Instant;
import java.util.UUID;

// maintenanceRecordId is null when the schedule has no linked MaintenanceRecord (unplanned-breakdown
// flow not yet derived into one) — consumers must no-op on null, matching ScheduleCompletionListener's
// existing no-op pattern for the symmetric completion event.
public record ScheduleCancelledEvent(
        UUID scheduleId,
        UUID maintenanceRecordId,
        Instant cancelledAt
) {}

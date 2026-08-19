package com.fleetmgm.gps.application;

import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsRetentionSchedulerTest {

    @Mock GpsRepository gpsRepository;
    @Mock AuditLogHelper auditLogHelper;

    @Test
    void purgeExpiredPositions_deletesEverythingRecordedBeforeTheRetentionWindow() {
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, auditLogHelper, 7);
        Instant startedAt = Instant.now();

        scheduler.purgeExpiredPositions();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(gpsRepository).deleteRecordedBefore(cutoff.capture());
        assertThat(cutoff.getValue())
                .isCloseTo(startedAt.minus(7, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeExpiredPositions_honoursTheConfiguredRetentionWindow() {
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, auditLogHelper, 30);
        Instant startedAt = Instant.now();

        scheduler.purgeExpiredPositions();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(gpsRepository).deleteRecordedBefore(cutoff.capture());
        assertThat(cutoff.getValue())
                .isCloseTo(startedAt.minus(30, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeExpiredPositions_reportsHowManyRowsWereRemoved() {
        when(gpsRepository.deleteRecordedBefore(any(Instant.class))).thenReturn(1_234);
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, auditLogHelper, 7);

        assertThat(scheduler.purgeExpiredPositions()).isEqualTo(1_234);
    }

    // A zero or negative window would put the cutoff at (or after) "now" and wipe every position
    // including the latest one per vehicle, blanking the live map. Fail at startup, not at 03:00.
    @Test
    void constructor_rejectsARetentionWindowShorterThanOneDay() {
        assertThatThrownBy(() -> new GpsRetentionScheduler(gpsRepository, auditLogHelper, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retention-days");
    }

    @Test
    void purgeExpiredPositions_recordsAnAuditLogEntryWhenRowsAreDeleted() {
        when(gpsRepository.deleteRecordedBefore(any(Instant.class))).thenReturn(42);
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, auditLogHelper, 7);

        scheduler.purgeExpiredPositions();

        verify(auditLogHelper).logSystem(eq("GpsPosition"), anyString(), eq(AuditAction.DELETE), anyString());
    }

    // Silence would defeat the point: an operator scanning the audit trail should not see a
    // stream of empty no-op sweeps drowning out the runs that actually removed data.
    @Test
    void purgeExpiredPositions_skipsTheAuditLogWhenNothingWasDeleted() {
        when(gpsRepository.deleteRecordedBefore(any(Instant.class))).thenReturn(0);
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, auditLogHelper, 7);

        scheduler.purgeExpiredPositions();

        verifyNoInteractions(auditLogHelper);
    }
}

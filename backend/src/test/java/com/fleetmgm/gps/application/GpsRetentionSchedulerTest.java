package com.fleetmgm.gps.application;

import com.fleetmgm.gps.infrastructure.GpsRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsRetentionSchedulerTest {

    @Mock GpsRepository gpsRepository;

    @Test
    void purgeExpiredPositions_deletesEverythingRecordedBeforeTheRetentionWindow() {
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, 7);
        Instant startedAt = Instant.now();

        scheduler.purgeExpiredPositions();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(gpsRepository).deleteRecordedBefore(cutoff.capture());
        assertThat(cutoff.getValue())
                .isCloseTo(startedAt.minus(7, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void purgeExpiredPositions_honoursTheConfiguredRetentionWindow() {
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, 30);
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
        GpsRetentionScheduler scheduler = new GpsRetentionScheduler(gpsRepository, 7);

        assertThat(scheduler.purgeExpiredPositions()).isEqualTo(1_234);
    }

    // A zero or negative window would put the cutoff at (or after) "now" and wipe every position
    // including the latest one per vehicle, blanking the live map. Fail at startup, not at 03:00.
    @Test
    void constructor_rejectsARetentionWindowShorterThanOneDay() {
        assertThatThrownBy(() -> new GpsRetentionScheduler(gpsRepository, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retention-days");
    }
}

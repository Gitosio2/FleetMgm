package com.fleetmgm.gps.application;

import com.fleetmgm.gps.infrastructure.GpsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Caps the unbounded growth of {@code gps_positions}. The fleet writes one row per active vehicle
 * every 30 seconds and nothing ever removed them, so the table grew for as long as the service ran —
 * roughly 2.880 rows per vehicle per day. Only the most recent position per vehicle is ever read
 * (the live map), so anything past the retention window is dead weight on a bounded disk.
 *
 * <p>Kept separate from {@link GpsMockScheduler}: that one fabricates demo data and would disappear
 * the day real GPS hardware feeds this table, whereas retention applies to any source.
 */
@Component
public class GpsRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(GpsRetentionScheduler.class);

    private final GpsRepository gpsRepository;
    private final int retentionDays;

    public GpsRetentionScheduler(GpsRepository gpsRepository,
                                 @Value("${gps.retention-days:7}") int retentionDays) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException(
                    "gps.retention-days must be at least 1, was " + retentionDays
                            + " — a window of zero would delete the latest position of every vehicle "
                            + "and blank the live map");
        }
        this.gpsRepository = gpsRepository;
        this.retentionDays = retentionDays;
    }

    /**
     * @return how many rows the sweep removed, so callers and tests can assert on the outcome.
     */
    // initialDelay keeps the sweep off the startup path, where it would compete with Flyway and the
    // first GPS tick for a cold connection pool.
    @Scheduled(initialDelay = 5, fixedDelay = 1_440, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public int purgeExpiredPositions() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = gpsRepository.deleteRecordedBefore(cutoff);
        if (deleted > 0) {
            log.info("GPS retention sweep removed {} position(s) recorded before {}", deleted, cutoff);
        }
        return deleted;
    }
}

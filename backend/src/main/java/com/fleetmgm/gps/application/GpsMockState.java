package com.fleetmgm.gps.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Whether {@link GpsMockScheduler} is currently allowed to write positions, held in memory rather
 * than in the database on purpose: the flag must come back <em>off</em> after every restart, redeploy
 * or container wake-up, so a forgotten "on" can never outlive the process that was demoing. Persisting
 * it would make the expensive state the sticky one, which is exactly backwards for a metered host.
 *
 * <p>Enabling is a lease, not a switch: it expires on its own after
 * {@code gps.mock.auto-disable-after-minutes} so that closing the browser tab is enough to stop the
 * writes. Re-enabling while already on renews the lease.
 */
@Component
public class GpsMockState {

    // Stands in for "on with no expiry" (auto-disable turned off via a 0-minute window), so the
    // single activeUntil reference can express all three states: null = off, MAX = on forever,
    // anything else = on until that instant.
    private static final Instant NO_EXPIRY = Instant.MAX;

    private final AtomicReference<Instant> activeUntil = new AtomicReference<>(null);
    private final Duration autoDisableAfter;
    private final Clock clock;

    // Two constructors, so Spring needs to be told which one to call. The Clock-taking one exists
    // for tests, which would otherwise have to sleep through a real auto-disable window.
    @Autowired
    public GpsMockState(@Value("${gps.mock.enabled-on-startup:false}") boolean enabledOnStartup,
                        @Value("${gps.mock.auto-disable-after-minutes:30}") int autoDisableAfterMinutes) {
        this(enabledOnStartup, autoDisableAfterMinutes, Clock.systemUTC());
    }

    GpsMockState(boolean enabledOnStartup, int autoDisableAfterMinutes, Clock clock) {
        if (autoDisableAfterMinutes < 0) {
            throw new IllegalArgumentException(
                    "gps.mock.auto-disable-after-minutes must be 0 (no auto-disable) or positive, was "
                            + autoDisableAfterMinutes);
        }
        this.autoDisableAfter = autoDisableAfterMinutes == 0 ? null : Duration.ofMinutes(autoDisableAfterMinutes);
        this.clock = clock;
        if (enabledOnStartup) {
            enable();
        }
    }

    public void enable() {
        activeUntil.set(autoDisableAfter == null ? NO_EXPIRY : clock.instant().plus(autoDisableAfter));
    }

    public void disable() {
        activeUntil.set(null);
    }

    public boolean isActive() {
        Instant until = activeUntil.get();
        return until != null && !hasLapsed(until);
    }

    /**
     * Clears a lease whose window has run out, reporting {@code true} to exactly one caller — the
     * tick that first observes the expiry — so the audit row for an auto-disable is written once
     * and not on every subsequent tick.
     */
    public boolean disableIfLapsed() {
        Instant until = activeUntil.get();
        return until != null && hasLapsed(until) && activeUntil.compareAndSet(until, null);
    }

    /**
     * @return when the current lease runs out, or {@code null} when the generator is off or is on
     *         with auto-disable turned off — both cases mean "no countdown to show".
     */
    public Instant activeUntil() {
        Instant until = activeUntil.get();
        if (until == null || until.equals(NO_EXPIRY) || hasLapsed(until)) {
            return null;
        }
        return until;
    }

    private boolean hasLapsed(Instant until) {
        return !clock.instant().isBefore(until);
    }
}

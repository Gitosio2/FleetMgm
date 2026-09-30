package com.fleetmgm.gps.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock the test moves by hand, so the auto-disable window can be tested at its real configured
 * size (minutes) without the test sleeping through it.
 */
final class AdvanceableClock extends Clock {

    private final ZoneId zone;
    private Instant now;

    AdvanceableClock(Instant start) {
        this(start, ZoneId.of("UTC"));
    }

    private AdvanceableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    void advance(Duration amount) {
        now = now.plus(amount);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        return new AdvanceableClock(now, otherZone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}

package com.fleetmgm.gps.dto;

import java.time.Instant;

/**
 * @param enabled     whether the mock generator is currently writing positions
 * @param enabledUntil when the current activation expires, or {@code null} when the generator is
 *                     off or was activated with auto-disable turned off
 * @param intervalSeconds how often a position is written per active vehicle while enabled
 */
public record GpsMockStatusResponse(boolean enabled, Instant enabledUntil, long intervalSeconds) {}

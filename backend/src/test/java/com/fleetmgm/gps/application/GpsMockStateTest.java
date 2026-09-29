package com.fleetmgm.gps.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GpsMockStateTest {

    private static final Instant START = Instant.parse("2026-09-29T10:00:00Z");
    private static final int WINDOW_MINUTES = 30;

    @Test
    void isActive_isFalse_byDefault() {
        assertThat(state(false).isActive()).isFalse();
    }

    // The whole point of the switch: a deployment that is redeployed, restarted or woken from sleep
    // must come back writing nothing, whatever it was doing before it went down.
    @Test
    void isActive_isFalse_afterRestart_evenIfItWasEnabledBefore() {
        GpsMockState before = state(false);
        before.enable();
        assertThat(before.isActive()).isTrue();

        assertThat(state(false).isActive()).isFalse();
    }

    @Test
    void isActive_isTrue_whenStartupFlagIsSet() {
        assertThat(state(true).isActive()).isTrue();
    }

    @Test
    void enable_thenDisable_stopsTheGenerator() {
        GpsMockState state = state(false);

        state.enable();
        assertThat(state.isActive()).isTrue();

        state.disable();
        assertThat(state.isActive()).isFalse();
        assertThat(state.activeUntil()).isNull();
    }

    @Test
    void enable_setsActiveUntil_oneWindowAhead() {
        AdvanceableClock clock = new AdvanceableClock(START);
        GpsMockState state = new GpsMockState(false, WINDOW_MINUTES, clock);

        state.enable();

        assertThat(state.activeUntil()).isEqualTo(START.plus(Duration.ofMinutes(WINDOW_MINUTES)));
    }

    @Test
    void isActive_isFalse_onceTheWindowHasLapsed() {
        AdvanceableClock clock = new AdvanceableClock(START);
        GpsMockState state = new GpsMockState(true, WINDOW_MINUTES, clock);

        clock.advance(Duration.ofMinutes(WINDOW_MINUTES - 1));
        assertThat(state.isActive()).isTrue();

        clock.advance(Duration.ofMinutes(1));
        assertThat(state.isActive()).isFalse();
        assertThat(state.activeUntil()).isNull();
    }

    @Test
    void enable_renewsTheWindow_whenAlreadyEnabled() {
        AdvanceableClock clock = new AdvanceableClock(START);
        GpsMockState state = new GpsMockState(true, WINDOW_MINUTES, clock);

        clock.advance(Duration.ofMinutes(WINDOW_MINUTES - 5));
        state.enable();
        clock.advance(Duration.ofMinutes(10));

        assertThat(state.isActive()).isTrue();
    }

    // Reported to exactly one caller so the audit row for an auto-disable is written once, not on
    // every tick that follows it.
    @Test
    void disableIfLapsed_reportsTheExpiry_onlyOnce() {
        AdvanceableClock clock = new AdvanceableClock(START);
        GpsMockState state = new GpsMockState(true, WINDOW_MINUTES, clock);

        assertThat(state.disableIfLapsed()).isFalse();

        clock.advance(Duration.ofMinutes(WINDOW_MINUTES));

        assertThat(state.disableIfLapsed()).isTrue();
        assertThat(state.disableIfLapsed()).isFalse();
    }

    @Test
    void disableIfLapsed_isFalse_whenAlreadyDisabled() {
        assertThat(state(false).disableIfLapsed()).isFalse();
    }

    @Test
    void zeroWindow_keepsTheGeneratorOn_withNoCountdownToShow() {
        AdvanceableClock clock = new AdvanceableClock(START);
        GpsMockState state = new GpsMockState(true, 0, clock);

        clock.advance(Duration.ofDays(7));

        assertThat(state.isActive()).isTrue();
        assertThat(state.activeUntil()).isNull();
    }

    @Test
    void negativeWindow_failsFast() {
        assertThatThrownBy(() -> new GpsMockState(false, -1, new AdvanceableClock(START)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("auto-disable-after-minutes");
    }

    private static GpsMockState state(boolean enabledOnStartup) {
        return new GpsMockState(enabledOnStartup, WINDOW_MINUTES, new AdvanceableClock(START));
    }
}

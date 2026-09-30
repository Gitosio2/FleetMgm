package com.fleetmgm.gps.application;

import com.fleetmgm.gps.dto.GpsMockStatusResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GpsMockServiceTest {

    private static final Instant START = Instant.parse("2026-09-29T10:00:00Z");
    private static final int WINDOW_MINUTES = 30;
    private static final long INTERVAL_MS = 30_000;

    @Mock AuditLogHelper auditLogHelper;

    @Test
    void status_reportsDisabled_whenTheGeneratorIsOff() {
        GpsMockStatusResponse status = service(state(false)).status();

        assertThat(status.enabled()).isFalse();
        assertThat(status.enabledUntil()).isNull();
        assertThat(status.intervalSeconds()).isEqualTo(30);
    }

    @Test
    void setEnabled_true_turnsTheGeneratorOn_andReportsWhenItWillLapse() {
        GpsMockState state = state(false);

        GpsMockStatusResponse status = service(state).setEnabled(true);

        assertThat(status.enabled()).isTrue();
        assertThat(status.enabledUntil()).isEqualTo(START.plus(Duration.ofMinutes(WINDOW_MINUTES)));
        assertThat(state.isActive()).isTrue();
    }

    @Test
    void setEnabled_false_turnsTheGeneratorOff() {
        GpsMockState state = state(true);

        GpsMockStatusResponse status = service(state).setEnabled(false);

        assertThat(status.enabled()).isFalse();
        assertThat(status.enabledUntil()).isNull();
        assertThat(state.isActive()).isFalse();
    }

    @Test
    void setEnabled_true_writesAnAuditRow() {
        service(state(false)).setEnabled(true);

        verify(auditLogHelper).log(eq("GpsMockGenerator"), eq("gps-mock-generator"), eq(AuditAction.UPDATE),
                contains("enabled until"));
    }

    @Test
    void setEnabled_false_writesAnAuditRow() {
        service(state(true)).setEnabled(false);

        verify(auditLogHelper).log(eq("GpsMockGenerator"), eq("gps-mock-generator"), eq(AuditAction.UPDATE),
                contains("disabled"));
    }

    private GpsMockService service(GpsMockState state) {
        return new GpsMockService(state, auditLogHelper, INTERVAL_MS);
    }

    private static GpsMockState state(boolean enabledOnStartup) {
        return new GpsMockState(enabledOnStartup, WINDOW_MINUTES, new AdvanceableClock(START));
    }
}

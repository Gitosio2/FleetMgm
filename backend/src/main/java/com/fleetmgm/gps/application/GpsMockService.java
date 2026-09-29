package com.fleetmgm.gps.application;

import com.fleetmgm.gps.dto.GpsMockStatusResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns the demo GPS feed on and off at runtime. Its own cost is one audit row per toggle; what it
 * controls is the only unbounded background cost in the deployment, so the read side is open to
 * everyone who can see the map and the write side is not.
 */
@Service
public class GpsMockService {

    static final String ENTITY_TYPE = "GpsMockGenerator";
    // Singleton control, not a row: a fixed id keeps every toggle on one audit trail instead of
    // scattering them across ids that refer to nothing.
    static final String ENTITY_ID = "gps-mock-generator";

    private final GpsMockState gpsMockState;
    private final AuditLogHelper auditLogHelper;
    private final long intervalMs;

    public GpsMockService(GpsMockState gpsMockState, AuditLogHelper auditLogHelper,
                          @Value("${gps.mock.interval-ms:30000}") long intervalMs) {
        this.gpsMockState = gpsMockState;
        this.auditLogHelper = auditLogHelper;
        this.intervalMs = intervalMs;
    }

    // No @Transactional: the state lives in memory, so this method touches no database at all and
    // an empty transaction would only take a pooled connection to do nothing with it.
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public GpsMockStatusResponse status() {
        return toResponse();
    }

    /**
     * Enabling while already enabled is not a no-op: it renews the auto-disable window, which is how
     * someone running a long demo keeps the feed alive without a second switch.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public GpsMockStatusResponse setEnabled(boolean enabled) {
        if (enabled) {
            gpsMockState.enable();
        } else {
            gpsMockState.disable();
        }

        GpsMockStatusResponse status = toResponse();
        auditLogHelper.log(ENTITY_TYPE, ENTITY_ID, AuditAction.UPDATE, describe(status));
        return status;
    }

    private String describe(GpsMockStatusResponse status) {
        if (!status.enabled()) {
            return "GPS mock generator disabled";
        }
        return status.enabledUntil() == null
                ? "GPS mock generator enabled with no auto-disable window"
                : "GPS mock generator enabled until " + status.enabledUntil();
    }

    private GpsMockStatusResponse toResponse() {
        return new GpsMockStatusResponse(gpsMockState.isActive(), gpsMockState.activeUntil(), intervalMs / 1000);
    }
}

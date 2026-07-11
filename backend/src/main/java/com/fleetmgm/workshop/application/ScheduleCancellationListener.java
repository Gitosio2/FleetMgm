package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceCancelledEvent;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.ScheduleCancelledEvent;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Lives in workshop.application, sibling to ScheduleCompletionListener — it mutates both
// WorkshopSchedule and MaintenanceRecord, both of which belong to this feature.
//
// Implements the bidirectional cascade from planning.md's "Adenda — Cancelación de mantenimiento":
// cancelling either side must cancel its linked counterpart, unless that counterpart is already
// terminal (COMPLETED/CANCELLED). Each handler checks the counterpart's CURRENT status itself and
// skips calling its cancel() entirely when there's nothing to do — this is what breaks the mutual
// cascade cycle (A cancels B -> event -> B's handler tries to cancel A -> A is already CANCELLED ->
// skip), rather than relying on cancel() throwing ConflictException and catching it.
@Component
public class ScheduleCancellationListener {

    private static final Logger log = LoggerFactory.getLogger(ScheduleCancellationListener.class);

    private final WorkshopScheduleRepository workshopScheduleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceService maintenanceService;
    private final WorkshopScheduleService workshopScheduleService;

    public ScheduleCancellationListener(WorkshopScheduleRepository workshopScheduleRepository,
                                        MaintenanceRepository maintenanceRepository,
                                        MaintenanceService maintenanceService,
                                        WorkshopScheduleService workshopScheduleService) {
        this.workshopScheduleRepository = workshopScheduleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.maintenanceService = maintenanceService;
        this.workshopScheduleService = workshopScheduleService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onScheduleCancelled(ScheduleCancelledEvent event) {
        // AFTER_COMMIT: the triggering transaction already committed and the original HTTP call
        // already returned 200 OK, so an exception here can't roll anything back — it must be
        // logged with enough context to debug later, never left silent and never rethrown.
        try {
            if (event.maintenanceRecordId() == null) {
                return;
            }
            maintenanceRepository.findById(event.maintenanceRecordId()).ifPresent(record -> {
                if (isTerminal(record.getStatus())) {
                    return;
                }
                maintenanceService.cancel(record.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to cascade-cancel maintenance record {} linked to schedule {}",
                    event.maintenanceRecordId(), event.scheduleId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onMaintenanceCancelled(MaintenanceCancelledEvent event) {
        try {
            workshopScheduleRepository.findByMaintenanceRecordId(event.maintenanceId()).ifPresent(schedule -> {
                if (isTerminal(schedule.getStatus())) {
                    return;
                }
                workshopScheduleService.cancel(schedule.getId());
            });
        } catch (RuntimeException e) {
            log.error("Failed to cascade-cancel workshop schedule linked to maintenance record {}",
                    event.maintenanceId(), e);
        }
    }

    private boolean isTerminal(MaintenanceStatus status) {
        return status == MaintenanceStatus.COMPLETED || status == MaintenanceStatus.CANCELLED;
    }

    private boolean isTerminal(WorkshopStatus status) {
        return status == WorkshopStatus.COMPLETED || status == WorkshopStatus.CANCELLED;
    }
}

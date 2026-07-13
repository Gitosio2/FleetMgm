package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceScheduledEvent;
import com.fleetmgm.workshop.dto.CreateScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Lives in workshop.application, sibling to ScheduleCompletionListener/ScheduleCancellationListener
// — it mutates WorkshopSchedule, which belongs to this feature — the rule established in Hito 21/24
// is "the listener lives in the package of the entity it mutates."
@Component
public class ScheduleCreationListener {

    private static final Logger log = LoggerFactory.getLogger(ScheduleCreationListener.class);

    private final WorkshopScheduleService workshopScheduleService;

    public ScheduleCreationListener(WorkshopScheduleService workshopScheduleService) {
        this.workshopScheduleService = workshopScheduleService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMaintenanceScheduled(MaintenanceScheduledEvent event) {
        // AFTER_COMMIT: the triggering transaction already committed and the original HTTP call
        // already returned 201, so an exception here can't roll anything back — it must be logged
        // with enough context to debug later, never left silent and never rethrown.
        try {
            workshopScheduleService.create(new CreateScheduleRequest(
                    event.vehicleId(), event.technicianId(), event.maintenanceId(),
                    event.scheduledDate(), event.type(), null, null, null, null));
        } catch (RuntimeException e) {
            log.error("Failed to auto-create workshop schedule for maintenance record {}",
                    event.maintenanceId(), e);
        }
    }
}

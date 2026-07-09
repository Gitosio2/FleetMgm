package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Lives in workshop.application (not vehicle.application, unlike MaintenanceEventListener) because it
// mutates WorkshopSchedule, which belongs to this feature — the rule established in Hito 21/24 is
// "the listener lives in the package of the entity it mutates."
@Component
public class ScheduleCompletionListener {

    private final WorkshopScheduleRepository workshopScheduleRepository;

    public ScheduleCompletionListener(WorkshopScheduleRepository workshopScheduleRepository) {
        this.workshopScheduleRepository = workshopScheduleRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onMaintenanceCompleted(MaintenanceCompletedEvent event) {
        // No manual /complete endpoint exists for WorkshopSchedule (Hito 25 decision) — a schedule only
        // becomes COMPLETED as a side effect of its linked maintenance record being completed. A schedule
        // with no linked maintenance record (maintenanceRecordId == null) is simply never found here.
        workshopScheduleRepository.findByMaintenanceRecordId(event.maintenanceId()).ifPresent(schedule -> {
            schedule.setStatus(WorkshopStatus.COMPLETED);
            workshopScheduleRepository.save(schedule);
        });
    }
}

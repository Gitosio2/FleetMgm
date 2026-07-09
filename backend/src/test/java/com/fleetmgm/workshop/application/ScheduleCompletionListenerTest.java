package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleCompletionListenerTest {

    @Mock WorkshopScheduleRepository workshopScheduleRepository;
    @InjectMocks ScheduleCompletionListener scheduleCompletionListener;

    @Test
    void onMaintenanceCompleted_completesLinkedSchedule() {
        UUID maintenanceId = UUID.randomUUID();
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setStatus(WorkshopStatus.IN_PROGRESS);
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.of(schedule));
        when(workshopScheduleRepository.save(schedule)).thenReturn(schedule);

        scheduleCompletionListener.onMaintenanceCompleted(event);

        assertThat(schedule.getStatus()).isEqualTo(WorkshopStatus.COMPLETED);
        verify(workshopScheduleRepository).save(schedule);
    }

    @Test
    void onMaintenanceCompleted_isNoOp_whenNoLinkedSchedule() {
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.empty());

        scheduleCompletionListener.onMaintenanceCompleted(event);

        verify(workshopScheduleRepository, never()).save(any());
    }
}

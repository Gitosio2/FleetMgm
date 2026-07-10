package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceCancelledEvent;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.ScheduleCancelledEvent;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Implements the bidirectional cascade documented in planning.md's "Adenda — Cancelación de
// mantenimiento": cancelling either WorkshopSchedule or MaintenanceRecord cascades to cancel its
// linked counterpart, unless that counterpart is already terminal (COMPLETED/CANCELLED). The
// terminal-status check happens BEFORE calling the counterpart's own cancel() (never after a
// ConflictException), which is what breaks the mutual A-cancels-B / B-tries-to-cancel-A cycle.
@ExtendWith(MockitoExtension.class)
class ScheduleCancellationListenerTest {

    @Mock WorkshopScheduleRepository workshopScheduleRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock MaintenanceService maintenanceService;
    @Mock WorkshopScheduleService workshopScheduleService;
    @InjectMocks ScheduleCancellationListener scheduleCancellationListener;

    // --- onScheduleCancelled: schedule -> linked maintenance ---

    @Test
    void onScheduleCancelled_cancelsLinkedMaintenance_whenNotTerminal() {
        UUID scheduleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        setId(record, maintenanceId);
        record.setStatus(MaintenanceStatus.SCHEDULED);
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(scheduleId, maintenanceId, Instant.now());

        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(record));

        scheduleCancellationListener.onScheduleCancelled(event);

        verify(maintenanceService).cancel(maintenanceId);
    }

    @Test
    void onScheduleCancelled_isNoOp_whenMaintenanceAlreadyCancelled() {
        UUID scheduleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.CANCELLED);
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(scheduleId, maintenanceId, Instant.now());

        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(record));

        scheduleCancellationListener.onScheduleCancelled(event);

        verify(maintenanceService, never()).cancel(any(UUID.class));
    }

    @Test
    void onScheduleCancelled_isNoOp_whenMaintenanceAlreadyCompleted() {
        UUID scheduleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.COMPLETED);
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(scheduleId, maintenanceId, Instant.now());

        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(record));

        scheduleCancellationListener.onScheduleCancelled(event);

        verify(maintenanceService, never()).cancel(any(UUID.class));
    }

    @Test
    void onScheduleCancelled_isNoOp_whenNoLinkedMaintenance() {
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(UUID.randomUUID(), null, Instant.now());

        scheduleCancellationListener.onScheduleCancelled(event);

        verifyNoInteractions(maintenanceRepository);
        verifyNoInteractions(maintenanceService);
    }

    @Test
    void onScheduleCancelled_isNoOp_whenMaintenanceNotFound() {
        UUID scheduleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(scheduleId, maintenanceId, Instant.now());

        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.empty());

        scheduleCancellationListener.onScheduleCancelled(event);

        verify(maintenanceService, never()).cancel(any(UUID.class));
    }

    // --- onMaintenanceCancelled: maintenance -> linked schedule ---

    @Test
    void onMaintenanceCancelled_cancelsLinkedSchedule_whenNotTerminal() {
        UUID maintenanceId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setStatus(WorkshopStatus.PENDING);
        setId(schedule, scheduleId);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.of(schedule));

        scheduleCancellationListener.onMaintenanceCancelled(event);

        verify(workshopScheduleService).cancel(scheduleId);
    }

    @Test
    void onMaintenanceCancelled_isNoOp_whenScheduleAlreadyCancelled() {
        UUID maintenanceId = UUID.randomUUID();
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setStatus(WorkshopStatus.CANCELLED);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.of(schedule));

        scheduleCancellationListener.onMaintenanceCancelled(event);

        verify(workshopScheduleService, never()).cancel(any(UUID.class));
    }

    @Test
    void onMaintenanceCancelled_isNoOp_whenScheduleAlreadyCompleted() {
        UUID maintenanceId = UUID.randomUUID();
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setStatus(WorkshopStatus.COMPLETED);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.of(schedule));

        scheduleCancellationListener.onMaintenanceCancelled(event);

        verify(workshopScheduleService, never()).cancel(any(UUID.class));
    }

    @Test
    void onMaintenanceCancelled_isNoOp_whenNoLinkedSchedule() {
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.empty());

        scheduleCancellationListener.onMaintenanceCancelled(event);

        verify(workshopScheduleService, never()).cancel(any(UUID.class));
    }

    // --- Bug/gap 2 (CRITICAL): AFTER_COMMIT listeners must never let an exception propagate — the
    // triggering transaction already committed and the original HTTP call already returned 200 OK,
    // so a cascade failure must be logged, not silently lost or rethrown. ---

    @Test
    void onScheduleCancelled_doesNotPropagate_whenMaintenanceServiceThrows() {
        UUID scheduleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        setId(record, maintenanceId);
        record.setStatus(MaintenanceStatus.SCHEDULED);
        ScheduleCancelledEvent event = new ScheduleCancelledEvent(scheduleId, maintenanceId, Instant.now());

        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(record));
        doThrow(new RuntimeException("boom")).when(maintenanceService).cancel(maintenanceId);

        assertThatCode(() -> scheduleCancellationListener.onScheduleCancelled(event))
                .doesNotThrowAnyException();
    }

    @Test
    void onMaintenanceCancelled_doesNotPropagate_whenWorkshopScheduleServiceThrows() {
        UUID maintenanceId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        WorkshopSchedule schedule = new WorkshopSchedule();
        schedule.setStatus(WorkshopStatus.PENDING);
        setId(schedule, scheduleId);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(maintenanceId, UUID.randomUUID(), Instant.now());

        when(workshopScheduleRepository.findByMaintenanceRecordId(maintenanceId)).thenReturn(Optional.of(schedule));
        doThrow(new RuntimeException("boom")).when(workshopScheduleService).cancel(scheduleId);

        assertThatCode(() -> scheduleCancellationListener.onMaintenanceCancelled(event))
                .doesNotThrowAnyException();
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}

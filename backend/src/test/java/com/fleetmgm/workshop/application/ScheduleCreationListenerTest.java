package com.fleetmgm.workshop.application;

import com.fleetmgm.workshop.domain.MaintenanceScheduledEvent;
import com.fleetmgm.workshop.dto.CreateScheduleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleCreationListenerTest {

    @Mock WorkshopScheduleService workshopScheduleService;
    @InjectMocks ScheduleCreationListener scheduleCreationListener;

    @Test
    void onMaintenanceScheduled_createsLinkedSchedule() {
        UUID maintenanceId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        LocalDate scheduledDate = LocalDate.now().plusDays(2);
        MaintenanceScheduledEvent event = new MaintenanceScheduledEvent(
                maintenanceId, vehicleId, technicianId, scheduledDate, "Oil change", Instant.now());

        scheduleCreationListener.onMaintenanceScheduled(event);

        ArgumentCaptor<CreateScheduleRequest> captor = ArgumentCaptor.forClass(CreateScheduleRequest.class);
        verify(workshopScheduleService).create(captor.capture());
        CreateScheduleRequest request = captor.getValue();
        assertThat(request.vehicleId()).isEqualTo(vehicleId);
        assertThat(request.technicianId()).isEqualTo(technicianId);
        assertThat(request.maintenanceRecordId()).isEqualTo(maintenanceId);
        assertThat(request.scheduledDate()).isEqualTo(scheduledDate);
        assertThat(request.type()).isEqualTo("Oil change");
        assertThat(request.priority()).isNull();
    }

    // AFTER_COMMIT listeners must never let an exception propagate — the triggering transaction
    // already committed and the original HTTP call already returned 201, so a failure here must be
    // logged, not silently lost or rethrown (mirrors ScheduleCancellationListenerTest).
    @Test
    void onMaintenanceScheduled_doesNotPropagate_whenWorkshopScheduleServiceThrows() {
        MaintenanceScheduledEvent event = new MaintenanceScheduledEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, LocalDate.now(), "Oil change", Instant.now());

        doThrow(new RuntimeException("boom")).when(workshopScheduleService).create(org.mockito.ArgumentMatchers.any());

        assertThatCode(() -> scheduleCreationListener.onMaintenanceScheduled(event))
                .doesNotThrowAnyException();
    }
}

package com.fleetmgm.vehicle.application;

import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.vehicle.domain.UsageLog;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.UsageSource;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.UsageLogRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobEventListenerTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock UsageLogRepository usageLogRepository;
    @Mock JobRepository jobRepository;
    @InjectMocks JobEventListener jobEventListener;

    // --- onJobCompleted ---

    @Test
    void onJobCompleted_updatesCurrentKm_forKilometersVehicle() {
        UUID vehicleId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        Job jobReference = new Job();
        JobCompletedEvent event = new JobCompletedEvent(jobId, vehicleId, 15000L, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(jobRepository.getReferenceById(jobId)).thenReturn(jobReference);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        jobEventListener.onJobCompleted(event);

        assertThat(vehicle.getCurrentKm()).isEqualTo(15000L);
        assertThat(vehicle.getCurrentHours()).isNull();
        verify(vehicleRepository).save(vehicle);

        ArgumentCaptor<UsageLog> captor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        UsageLog savedLog = captor.getValue();
        assertThat(savedLog.getVehicle()).isEqualTo(vehicle);
        assertThat(savedLog.getValue()).isEqualTo(15000L);
        assertThat(savedLog.getMeasureType()).isEqualTo(UsageMeasure.KILOMETERS);
        assertThat(savedLog.getSource()).isEqualTo(UsageSource.JOB_COMPLETION);
        assertThat(savedLog.getJob()).isEqualTo(jobReference);
    }

    @Test
    void onJobCompleted_updatesCurrentHours_forHoursVehicle() {
        UUID vehicleId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.HOURS);
        JobCompletedEvent event = new JobCompletedEvent(jobId, vehicleId, 320L, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(jobRepository.getReferenceById(jobId)).thenReturn(new Job());
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

        jobEventListener.onJobCompleted(event);

        assertThat(vehicle.getCurrentHours()).isEqualTo(320L);
        assertThat(vehicle.getCurrentKm()).isNull();

        ArgumentCaptor<UsageLog> captor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMeasureType()).isEqualTo(UsageMeasure.HOURS);
    }

    @Test
    void onJobCompleted_doesNothing_whenEndUsageValueIsNull() {
        JobCompletedEvent event = new JobCompletedEvent(UUID.randomUUID(), UUID.randomUUID(), null, Instant.now());

        jobEventListener.onJobCompleted(event);

        verifyNoInteractions(vehicleRepository);
        verifyNoInteractions(usageLogRepository);
        verifyNoInteractions(jobRepository);
    }

    @Test
    void onJobCompleted_skipsUpdate_whenEndUsageValueRegressesBelowCurrent() {
        UUID vehicleId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setCurrentKm(10000L);
        JobCompletedEvent event = new JobCompletedEvent(jobId, vehicleId, 9000L, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        jobEventListener.onJobCompleted(event);

        assertThat(vehicle.getCurrentKm()).isEqualTo(10000L);
        verify(vehicleRepository, never()).save(any());
        verifyNoInteractions(usageLogRepository);
        verifyNoInteractions(jobRepository);
    }
}

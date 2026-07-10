package com.fleetmgm.job.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.job.dto.CreateJobRequest;
import com.fleetmgm.job.dto.JobMapper;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock WorkerRepository workerRepository;
    @Mock ClientRepository clientRepository;
    @Mock UserRepository userRepository;
    @Mock JobMapper jobMapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks JobService jobService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsWithPendingStatusAndWiredRelations_whenValid() {
        UUID vehicleId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        CreateJobRequest request = new CreateJobRequest(vehicleId, driverId, clientId, "Delivery", null,
                "Origin", "Destination", null, null, null);

        Vehicle vehicle = new Vehicle();
        Worker driver = new Worker();
        Client client = new Client();
        Job entity = new Job();
        JobResponse expected = buildJobResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(jobMapper.toEntity(request)).thenReturn(entity);
        when(jobRepository.save(entity)).thenReturn(entity);
        when(jobMapper.toResponse(entity)).thenReturn(expected);

        JobResponse result = jobService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(captor.getValue().getAssignedDriver()).isEqualTo(driver);
        assertThat(captor.getValue().getClient()).isEqualTo(client);
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void create_throwsNotFound_whenVehicleMissing() {
        UUID vehicleId = UUID.randomUUID();
        CreateJobRequest request = new CreateJobRequest(vehicleId, null, null, "Delivery", null,
                "Origin", "Destination", null, null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

        verify(jobRepository, never()).save(any());
    }

    // --- start ---

    @Test
    void start_transitionsToInProgress_whenPending() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setStatus(JobStatus.PENDING);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.start(id, 1000L);

        assertThat(result).isEqualTo(expected);
        assertThat(job.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(job.getActualStart()).isNotNull();
        assertThat(job.getStartUsageValue()).isEqualTo(1000L);
    }

    @Test
    void start_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.start(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("JOB_INVALID_STATE_TRANSITION"));

        verify(jobRepository, never()).save(any());
    }

    // --- complete ---

    @Test
    void complete_publishesJobCompletedEvent_withCorrectPayload() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(vehicleId);
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setVehicle(vehicle);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.complete(id, 5000L);

        assertThat(result).isEqualTo(expected);
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getActualEnd()).isNotNull();
        assertThat(job.getEndUsageValue()).isEqualTo(5000L);

        ArgumentCaptor<JobCompletedEvent> captor = ArgumentCaptor.forClass(JobCompletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        JobCompletedEvent event = captor.getValue();
        assertThat(event.vehicleId()).isEqualTo(vehicleId);
        assertThat(event.endUsageValue()).isEqualTo(5000L);
        assertThat(event.completedAt()).isEqualTo(job.getActualEnd());
    }

    @Test
    void complete_throwsConflict_whenNotInProgress() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setStatus(JobStatus.PENDING);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.complete(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("JOB_INVALID_STATE_TRANSITION"));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void complete_throwsConflict_whenEndUsageValueBelowVehicleCurrent() {
        UUID id = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setCurrentKm(10_000L);
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setVehicle(vehicle);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.complete(id, 9_000L))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("JOB_USAGE_VALUE_BELOW_CURRENT"));

        assertThat(job.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        verify(jobRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void complete_throwsConflict_whenEndUsageValueBelowJobStartValue() {
        UUID id = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.KILOMETERS);
        vehicle.setCurrentKm(1_000L);
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setVehicle(vehicle);
        job.setStartUsageValue(5_000L);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.complete(id, 4_000L))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("JOB_USAGE_VALUE_BELOW_CURRENT"));

        verify(jobRepository, never()).save(any());
    }

    @Test
    void complete_succeeds_whenEndUsageValueEqualsCurrentVehicleValue() {
        UUID id = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setUsageMeasure(UsageMeasure.HOURS);
        vehicle.setCurrentHours(2_000L);
        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setVehicle(vehicle);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.complete(id, 2_000L);

        assertThat(result).isEqualTo(expected);
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    // --- cancel ---

    @Test
    void cancel_throwsConflict_whenAlreadyCompleted() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setStatus(JobStatus.COMPLETED);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.cancel(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("JOB_INVALID_STATE_TRANSITION"));

        verify(jobRepository, never()).save(any());
    }

    @Test
    void cancel_succeeds_whenPending() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setStatus(JobStatus.PENDING);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.cancel(id);

        assertThat(result).isEqualTo(expected);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    // --- driver ownership ---

    @Test
    void start_asDriver_succeeds_whenJobAssignedToCaller() {
        setDriverAuthentication("driver@example.com");
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(userId);
        Worker driver = mock(Worker.class);
        when(driver.getId()).thenReturn(driverId);

        Job job = new Job();
        job.setStatus(JobStatus.PENDING);
        job.setAssignedDriver(driver);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(mockedUser));
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(jobRepository.save(job)).thenReturn(job);
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.start(id, null);

        assertThat(result).isEqualTo(expected);
        assertThat(job.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
    }

    @Test
    void start_asDriver_throwsAccessDenied_whenJobAssignedToDifferentDriver() {
        setDriverAuthentication("driver@example.com");
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID callerDriverId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();

        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(userId);
        Worker caller = mock(Worker.class);
        when(caller.getId()).thenReturn(callerDriverId);
        Worker otherDriver = mock(Worker.class);
        when(otherDriver.getId()).thenReturn(otherDriverId);

        Job job = new Job();
        job.setStatus(JobStatus.PENDING);
        job.setAssignedDriver(otherDriver);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(mockedUser));
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.of(caller));

        assertThatThrownBy(() -> jobService.start(id, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void getById_asDriver_throwsAccessDenied_whenOwnJobIsCompleted() {
        setDriverAuthentication("driver@example.com");
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(userId);
        Worker driver = mock(Worker.class);
        when(driver.getId()).thenReturn(driverId);

        Job job = new Job();
        job.setStatus(JobStatus.COMPLETED);
        job.setAssignedDriver(driver);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(mockedUser));
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> jobService.getById(id))
                .isInstanceOf(AccessDeniedException.class);

        verify(jobMapper, never()).toResponse(any(Job.class));
    }

    @Test
    void getById_asDriver_succeeds_whenOwnJobIsInProgress() {
        setDriverAuthentication("driver@example.com");
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(userId);
        Worker driver = mock(Worker.class);
        when(driver.getId()).thenReturn(driverId);

        Job job = new Job();
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setAssignedDriver(driver);
        JobResponse expected = buildJobResponse(id);

        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(mockedUser));
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.of(driver));
        when(jobMapper.toResponse(job)).thenReturn(expected);

        JobResponse result = jobService.getById(id);

        assertThat(result).isEqualTo(expected);
    }

    // --- helpers ---

    private void setDriverAuthentication(String email) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private JobResponse buildJobResponse(UUID id) {
        return new JobResponse(id, "Delivery", null, UUID.randomUUID(), "1234ABC", null, null,
                null, null, null, null, JobStatus.PENDING,
                "Origin", "Destination", null, null, null, null, null, null, null, Instant.now());
    }
}

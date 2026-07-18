package com.fleetmgm.workshop.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.exception.BadRequestException;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.ScheduleCancelledEvent;
import com.fleetmgm.workshop.domain.ScheduleRange;
import com.fleetmgm.workshop.domain.SchedulePriority;
import com.fleetmgm.workshop.domain.WorkshopSchedule;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.dto.CreateScheduleRequest;
import com.fleetmgm.workshop.dto.ScheduleMapper;
import com.fleetmgm.workshop.dto.ScheduleResponse;
import com.fleetmgm.workshop.dto.UpdateScheduleRequest;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopScheduleServiceTest {

    @Mock WorkshopScheduleRepository workshopScheduleRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock WorkerRepository workerRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock MaintenanceService maintenanceService;
    @Mock ScheduleMapper scheduleMapper;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks WorkshopScheduleService workshopScheduleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsPendingSchedule() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null, null, null, null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(captor.getValue().getStatus()).isEqualTo(WorkshopStatus.PENDING);
    }

    @Test
    void create_defaultsToMedium_whenPriorityNotProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null, null, null, null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.create(request);

        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(SchedulePriority.MEDIUM);
    }

    @Test
    void create_persistsProvidedPriority_whenGiven() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", SchedulePriority.URGENT, null, null, null, null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.create(request);

        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(SchedulePriority.URGENT);
    }

    @Test
    void create_throwsNotFound_whenVehicleMissing() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null, null, null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    @Test
    void create_wiresTechnician_whenProvided() {
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, technicianId, null, LocalDate.now(), "Oil change", null, null, null, null, null);

        Vehicle vehicle = new Vehicle();
        Worker technician = new Worker();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.create(request);

        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getTechnician()).isEqualTo(technician);
    }

    @Test
    void create_throwsNotFound_whenTechnicianMissing() {
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, technicianId, null, LocalDate.now(), "Oil change", null, null, null, null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("WORKER_NOT_FOUND"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    @Test
    void create_wiresMaintenanceRecord_whenProvided() {
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, maintenanceId, LocalDate.now(), "Oil change", null, null, null, null, null);

        Vehicle vehicle = new Vehicle();
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.of(maintenanceRecord));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.create(request);

        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getMaintenanceRecord()).isEqualTo(maintenanceRecord);
        verify(maintenanceService, never()).createFromSchedule(any(), any(), any(), any());
    }

    @Test
    void create_createsLinkedMaintenanceRecord_whenNoMaintenanceRecordIdProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Rotura de transmisión", null, null, null, null,
                MaintenanceCategory.CORRECTIVE);

        Vehicle vehicle = new Vehicle();
        MaintenanceRecord created = new MaintenanceRecord();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceService.createFromSchedule(vehicle, null, "Rotura de transmisión", MaintenanceCategory.CORRECTIVE))
                .thenReturn(created);
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.create(request);

        ArgumentCaptor<WorkshopSchedule> captor = ArgumentCaptor.forClass(WorkshopSchedule.class);
        verify(workshopScheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getMaintenanceRecord()).isEqualTo(created);
        verify(maintenanceRepository, never()).findById(any());
    }

    @Test
    void create_throwsNotFound_whenMaintenanceRecordMissing() {
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, maintenanceId, LocalDate.now(), "Oil change", null, null, null, null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(maintenanceRepository.findById(maintenanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MAINTENANCE_NOT_FOUND"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    // --- create: time range (Hito 28) ---

    @Test
    void create_persists_whenOnlyStartTimeProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null,
                java.time.LocalTime.of(9, 0), null, null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(workshopScheduleRepository).save(entity);
    }

    @Test
    void create_persists_whenOnlyEndTimeProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null,
                null, java.time.LocalTime.of(17, 0), null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(workshopScheduleRepository).save(entity);
    }

    @Test
    void create_persists_whenBothTimesProvidedInValidOrder() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0), null);

        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(scheduleMapper.toEntity(request)).thenReturn(entity);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(workshopScheduleRepository).save(entity);
    }

    @Test
    void create_throwsBadRequest_whenEndTimeNotAfterStartTime() {
        UUID vehicleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null,
                java.time.LocalTime.of(17, 0), java.time.LocalTime.of(9, 0), null);

        assertThatThrownBy(() -> workshopScheduleService.create(request))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("SCHEDULE_INVALID_TIME_RANGE"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    @Test
    void create_throwsBadRequest_whenEndTimeEqualsStartTime() {
        UUID vehicleId = UUID.randomUUID();
        java.time.LocalTime same = java.time.LocalTime.of(10, 0);
        CreateScheduleRequest request = new CreateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Oil change", null, null, same, same, null);

        assertThatThrownBy(() -> workshopScheduleService.create(request))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("SCHEDULE_INVALID_TIME_RANGE"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    @Test
    void create_allowsOverlappingTimeRanges_forSameTechnician_noValidationPerformed() {
        // Explicit decision (Hito 28): overlap between two schedules for the same technician is
        // NOT validated at all — both must succeed. This locks in that behavior so it isn't
        // accidentally reintroduced later.
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        CreateScheduleRequest firstRequest = new CreateScheduleRequest(
                vehicleId, technicianId, null, LocalDate.now(), "Oil change", null, null,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(11, 0), null);
        CreateScheduleRequest secondRequest = new CreateScheduleRequest(
                vehicleId, technicianId, null, LocalDate.now(), "Brake check", null, null,
                java.time.LocalTime.of(10, 0), java.time.LocalTime.of(12, 0), null);

        Vehicle vehicle = new Vehicle();
        Worker technician = new Worker();
        WorkshopSchedule firstEntity = new WorkshopSchedule();
        WorkshopSchedule secondEntity = new WorkshopSchedule();
        ScheduleResponse firstExpected = buildResponse(UUID.randomUUID());
        ScheduleResponse secondExpected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(scheduleMapper.toEntity(firstRequest)).thenReturn(firstEntity);
        when(scheduleMapper.toEntity(secondRequest)).thenReturn(secondEntity);
        when(workshopScheduleRepository.save(firstEntity)).thenReturn(firstEntity);
        when(workshopScheduleRepository.save(secondEntity)).thenReturn(secondEntity);
        when(scheduleMapper.toResponse(firstEntity)).thenReturn(firstExpected);
        when(scheduleMapper.toResponse(secondEntity)).thenReturn(secondExpected);

        ScheduleResponse firstResult = workshopScheduleService.create(firstRequest);
        ScheduleResponse secondResult = workshopScheduleService.create(secondRequest);

        assertThat(firstResult).isEqualTo(firstExpected);
        assertThat(secondResult).isEqualTo(secondExpected);
        verify(workshopScheduleRepository).save(firstEntity);
        verify(workshopScheduleRepository).save(secondEntity);
    }

    // --- update: time range (Hito 28) ---

    @Test
    void update_persists_whenBothTimesProvidedInValidOrder() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Brake check", SchedulePriority.HIGH, null,
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(17, 0));
        WorkshopSchedule entity = new WorkshopSchedule();
        Vehicle vehicle = new Vehicle();
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.update(id, request);

        assertThat(result).isEqualTo(expected);
        verify(workshopScheduleRepository).save(entity);
    }

    @Test
    void update_throwsBadRequest_whenEndTimeNotAfterStartTime() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                vehicleId, null, null, LocalDate.now(), "Brake check", SchedulePriority.HIGH, null,
                java.time.LocalTime.of(17, 0), java.time.LocalTime.of(9, 0));

        assertThatThrownBy(() -> workshopScheduleService.update(id, request))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("SCHEDULE_INVALID_TIME_RANGE"));

        verify(workshopScheduleRepository, never()).findById(any());
        verify(workshopScheduleRepository, never()).save(any());
    }

    // --- listByRange ---

    @Test
    void listByRange_today_queriesSingleDayWindow() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate today = LocalDate.now();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(workshopScheduleRepository.findAllByScheduledDateBetween(eq(today), eq(today), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        PageResponse<ScheduleResponse> result = workshopScheduleService.listByRange(ScheduleRange.TODAY, pageable);

        assertThat(result.content()).containsExactly(expected);
    }

    @Test
    void listByRange_week_queriesMondayToSundayWindow() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(workshopScheduleRepository.findAllByScheduledDateBetween(eq(monday), eq(sunday), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        PageResponse<ScheduleResponse> result = workshopScheduleService.listByRange(ScheduleRange.WEEK, pageable);

        assertThat(result.content()).containsExactly(expected);
    }

    @Test
    void listByRange_month_queriesFirstToLastDayOfMonth() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(UUID.randomUUID());

        when(workshopScheduleRepository.findAllByScheduledDateBetween(eq(firstDay), eq(lastDay), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        PageResponse<ScheduleResponse> result = workshopScheduleService.listByRange(ScheduleRange.MONTH, pageable);

        assertThat(result.content()).containsExactly(expected);
    }

    // --- getById ---

    @Test
    void getById_returnsMappedSchedule_whenFound() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        assertThat(workshopScheduleService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_updatesFieldsAndRewiresRelations_whenValid() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                vehicleId, technicianId, null, LocalDate.now(), "Brake check", SchedulePriority.HIGH, null, null, null);
        WorkshopSchedule entity = new WorkshopSchedule();
        Vehicle vehicle = new Vehicle();
        Worker technician = new Worker();
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.update(id, request);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getVehicle()).isEqualTo(vehicle);
        assertThat(entity.getTechnician()).isEqualTo(technician);
        verify(scheduleMapper).updateEntity(request, entity);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                UUID.randomUUID(), null, null, LocalDate.now(), "Brake check", SchedulePriority.MEDIUM, null, null, null);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.update(id, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SCHEDULE_NOT_FOUND"));

        verify(workshopScheduleRepository, never()).save(any());
    }

    // --- start ---

    @Test
    void start_transitionsToInProgress_whenPending() {
        UUID id = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        entity.setVehicle(vehicle);
        entity.setType("Oil change");
        ScheduleResponse expected = buildResponse(id);
        MaintenanceRecord created = new MaintenanceRecord();
        setId(created, maintenanceId);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(maintenanceService.createFromSchedule(vehicle, null, "Oil change", null)).thenReturn(created);
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.start(id);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getStatus()).isEqualTo(WorkshopStatus.IN_PROGRESS);
        assertThat(entity.getMaintenanceRecord()).isEqualTo(created);
        verify(maintenanceService).start(maintenanceId, null);
    }

    @Test
    void start_cascadesToLinkedScheduledMaintenance_withoutCreatingANewOne() {
        UUID id = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord linked = new MaintenanceRecord();
        setId(linked, maintenanceId);
        linked.setStatus(MaintenanceStatus.SCHEDULED);
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        entity.setMaintenanceRecord(linked);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.start(id);

        verify(maintenanceService).start(maintenanceId, null);
        verify(maintenanceService, never()).createFromSchedule(any(), any(), any(), any());
    }

    @Test
    void start_doesNotCascade_whenLinkedMaintenanceAlreadyStarted() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord linked = new MaintenanceRecord();
        linked.setStatus(MaintenanceStatus.IN_PROGRESS);
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        entity.setMaintenanceRecord(linked);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.start(id);

        verify(maintenanceService, never()).start(any(), any());
        verify(maintenanceService, never()).createFromSchedule(any(), any(), any(), any());
    }

    @Test
    void start_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.IN_PROGRESS);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> workshopScheduleService.start(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SCHEDULE_INVALID_STATE_TRANSITION"));

        verify(workshopScheduleRepository, never()).save(any());
        verify(maintenanceService, never()).createFromSchedule(any(), any(), any(), any());
    }

    // --- completeLinkedMaintenance ---

    @Test
    void completeLinkedMaintenance_delegatesToMaintenanceService() {
        UUID id = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord linked = new MaintenanceRecord();
        setId(linked, maintenanceId);
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setMaintenanceRecord(linked);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));

        workshopScheduleService.completeLinkedMaintenance(id);

        verify(maintenanceService).complete(maintenanceId, null);
    }

    @Test
    void completeLinkedMaintenance_throwsConflict_whenNoLinkedMaintenance() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> workshopScheduleService.completeLinkedMaintenance(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SCHEDULE_NO_LINKED_MAINTENANCE"));

        verify(maintenanceService, never()).complete(any(), any());
    }

    @Test
    void completeLinkedMaintenance_throwsNotFound_whenScheduleMissing() {
        UUID id = UUID.randomUUID();
        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopScheduleService.completeLinkedMaintenance(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SCHEDULE_NOT_FOUND"));
    }

    // --- cancel ---

    @Test
    void cancel_transitionsToCancelled_andPublishesEvent_whenPending() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.cancel(id);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getStatus()).isEqualTo(WorkshopStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(ScheduleCancelledEvent.class));
    }

    @Test
    void cancel_transitionsToCancelled_andPublishesEvent_whenInProgress() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.IN_PROGRESS);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        ScheduleResponse result = workshopScheduleService.cancel(id);

        assertThat(result).isEqualTo(expected);
        assertThat(entity.getStatus()).isEqualTo(WorkshopStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(ScheduleCancelledEvent.class));
    }

    @Test
    void cancel_publishesEventWithMaintenanceRecordId_whenLinked() {
        UUID id = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        setId(maintenanceRecord, maintenanceId);
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        entity.setMaintenanceRecord(maintenanceRecord);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.cancel(id);

        ArgumentCaptor<ScheduleCancelledEvent> captor = ArgumentCaptor.forClass(ScheduleCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().maintenanceRecordId()).isEqualTo(maintenanceId);
    }

    @Test
    void cancel_publishesEventWithNullMaintenanceRecordId_whenNotLinked() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.PENDING);
        ScheduleResponse expected = buildResponse(id);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workshopScheduleRepository.save(entity)).thenReturn(entity);
        when(scheduleMapper.toResponse(entity)).thenReturn(expected);

        workshopScheduleService.cancel(id);

        ArgumentCaptor<ScheduleCancelledEvent> captor = ArgumentCaptor.forClass(ScheduleCancelledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().maintenanceRecordId()).isNull();
    }

    @Test
    void cancel_throwsConflict_whenAlreadyCompleted() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.COMPLETED);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> workshopScheduleService.cancel(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SCHEDULE_INVALID_STATE_TRANSITION"));

        verify(workshopScheduleRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletesPendingSchedule_andWritesAuditLog() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        setId(entity, id);
        entity.setStatus(WorkshopStatus.PENDING);

        User user = new User();
        setId(user, userId);
        user.setEmail("staff@fleetmgm.com");

        setAuthentication("staff@fleetmgm.com");
        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userRepository.findByEmail("staff@fleetmgm.com")).thenReturn(Optional.of(user));

        workshopScheduleService.delete(id);

        assertThat(entity.getDeletedAt()).isNotNull();
        verify(workshopScheduleRepository).save(entity);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(log.getEntityType()).isEqualTo("WorkshopSchedule");
        assertThat(log.getEntityId()).isEqualTo(id.toString());
        assertThat(log.getPerformedByEmail()).isEqualTo("staff@fleetmgm.com");
        assertThat(log.getPerformedByUserId()).isEqualTo(userId);
    }

    @Test
    void delete_throwsConflict_whenNotPending() {
        UUID id = UUID.randomUUID();
        WorkshopSchedule entity = new WorkshopSchedule();
        entity.setStatus(WorkshopStatus.IN_PROGRESS);

        when(workshopScheduleRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> workshopScheduleService.delete(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("SCHEDULE_DELETE_NOT_ALLOWED"));

        verify(workshopScheduleRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    private static void setAuthentication(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
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

    private ScheduleResponse buildResponse(UUID id) {
        return new ScheduleResponse(id, UUID.randomUUID(), "1234-ABC", null, null, null, null, null, null,
                LocalDate.now(), "Oil change", SchedulePriority.MEDIUM, WorkshopStatus.PENDING, null, null, null, null);
    }
}

package com.fleetmgm.workshop.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.VehicleEntersWorkshopEvent;
import com.fleetmgm.workshop.dto.CompleteMaintenanceRequest;
import com.fleetmgm.workshop.dto.CreateMaintenanceRequest;
import com.fleetmgm.workshop.dto.MaintenanceMapper;
import com.fleetmgm.workshop.dto.MaintenanceResponse;
import com.fleetmgm.workshop.dto.StartMaintenanceRequest;
import com.fleetmgm.workshop.dto.UpdateMaintenanceRequest;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock MaintenanceRepository maintenanceRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock WorkerRepository workerRepository;
    @Mock MaintenanceMapper maintenanceMapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @InjectMocks MaintenanceService maintenanceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsScheduledRecord_withoutPublishingEvent() {
        UUID vehicleId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(vehicleId, "Oil change", null, null, null);

        Vehicle vehicle = new Vehicle();
        MaintenanceRecord entity = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceMapper.toEntity(request)).thenReturn(entity);
        when(maintenanceRepository.save(entity)).thenReturn(entity);
        when(maintenanceMapper.toResponse(entity)).thenReturn(expected);

        MaintenanceResponse result = maintenanceService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRepository).save(captor.capture());
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(captor.getValue().getStatus()).isEqualTo(MaintenanceStatus.SCHEDULED);
        // creating a maintenance must NOT move the vehicle into the workshop — that happens on start()
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_defaultsToPreventive_whenCategoryNotProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(vehicleId, "Oil change", null, null, null);

        Vehicle vehicle = new Vehicle();
        MaintenanceRecord entity = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceMapper.toEntity(request)).thenReturn(entity);
        when(maintenanceRepository.save(entity)).thenReturn(entity);
        when(maintenanceMapper.toResponse(entity)).thenReturn(expected);

        maintenanceService.create(request);

        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(MaintenanceCategory.PREVENTIVE);
    }

    @Test
    void create_persistsCorrective_whenProvided() {
        UUID vehicleId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(
                vehicleId, "Brake repair", null, null, MaintenanceCategory.CORRECTIVE);

        Vehicle vehicle = new Vehicle();
        MaintenanceRecord entity = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceMapper.toEntity(request)).thenReturn(entity);
        when(maintenanceRepository.save(entity)).thenReturn(entity);
        when(maintenanceMapper.toResponse(entity)).thenReturn(expected);

        maintenanceService.create(request);

        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(MaintenanceCategory.CORRECTIVE);
    }

    @Test
    void create_throwsNotFound_whenVehicleMissing() {
        UUID vehicleId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(vehicleId, "Oil change", null, null, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));

        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void create_wiresTechnician_whenProvided() {
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(vehicleId, "Oil change", null, technicianId, null);

        Vehicle vehicle = new Vehicle();
        Worker technician = new Worker();
        MaintenanceRecord entity = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(UUID.randomUUID());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(maintenanceMapper.toEntity(request)).thenReturn(entity);
        when(maintenanceRepository.save(entity)).thenReturn(entity);
        when(maintenanceMapper.toResponse(entity)).thenReturn(expected);

        maintenanceService.create(request);

        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(maintenanceRepository).save(captor.capture());
        assertThat(captor.getValue().getTechnician()).isEqualTo(technician);
    }

    @Test
    void create_throwsNotFound_whenTechnicianMissing() {
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        CreateMaintenanceRequest request = new CreateMaintenanceRequest(vehicleId, "Oil change", null, technicianId, null);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.create(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("WORKER_NOT_FOUND"));

        verify(maintenanceRepository, never()).save(any());
    }

    // --- start ---

    @Test
    void start_transitionsToInProgress_andPublishesEvent_whenScheduled() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.SCHEDULED);
        record.setVehicle(new Vehicle());
        MaintenanceResponse expected = buildResponse(id);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));
        when(maintenanceRepository.save(record)).thenReturn(record);
        when(maintenanceMapper.toResponse(record)).thenReturn(expected);

        MaintenanceResponse result = maintenanceService.start(id, new StartMaintenanceRequest(15000L));

        assertThat(result).isEqualTo(expected);
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(record.getWorkshopEntryDate()).isNotNull();
        assertThat(record.getUsageAtService()).isEqualTo(15000L);
        verify(eventPublisher).publishEvent(any(VehicleEntersWorkshopEvent.class));
    }

    @Test
    void start_throwsConflict_whenNotScheduled() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.IN_PROGRESS);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> maintenanceService.start(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("MAINTENANCE_INVALID_STATE_TRANSITION"));

        verify(maintenanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- complete ---

    @Test
    void complete_transitionsToCompleted_andPublishesEvent_whenInProgress() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.IN_PROGRESS);
        record.setVehicle(new Vehicle());
        MaintenanceResponse expected = buildResponse(id);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));
        when(maintenanceRepository.save(record)).thenReturn(record);
        when(maintenanceMapper.toResponse(record)).thenReturn(expected);

        MaintenanceResponse result = maintenanceService.complete(id, new CompleteMaintenanceRequest(new BigDecimal("250.00")));

        assertThat(result).isEqualTo(expected);
        assertThat(record.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(record.getWorkshopExitDate()).isNotNull();
        assertThat(record.getCost()).isEqualByComparingTo("250.00");
        verify(eventPublisher).publishEvent(any(MaintenanceCompletedEvent.class));
    }

    @Test
    void complete_throwsConflict_whenNotInProgress() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.SCHEDULED);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> maintenanceService.complete(id, null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("MAINTENANCE_INVALID_STATE_TRANSITION"));

        verify(maintenanceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- list ---

    @Test
    void list_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        MaintenanceRecord record = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(UUID.randomUUID());

        when(maintenanceRepository.findAllJoinFetch(pageable))
                .thenReturn(new PageImpl<>(List.of(record), pageable, 1));
        when(maintenanceMapper.toResponse(record)).thenReturn(expected);

        PageResponse<MaintenanceResponse> result = maintenanceService.list(pageable);

        assertThat(result.content()).containsExactly(expected);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // --- getById ---

    @Test
    void getById_returnsMappedRecord_whenFound() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        MaintenanceResponse expected = buildResponse(id);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));
        when(maintenanceMapper.toResponse(record)).thenReturn(expected);

        assertThat(maintenanceService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(maintenanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MAINTENANCE_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_updatesFieldsAndRewiresRelations_whenValid() {
        UUID id = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        UpdateMaintenanceRequest request = new UpdateMaintenanceRequest(
                vehicleId, "Brake check", "front pads", technicianId, MaintenanceCategory.CORRECTIVE);
        MaintenanceRecord record = new MaintenanceRecord();
        Vehicle vehicle = new Vehicle();
        Worker technician = new Worker();
        MaintenanceResponse expected = buildResponse(id);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(workerRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(maintenanceRepository.save(record)).thenReturn(record);
        when(maintenanceMapper.toResponse(record)).thenReturn(expected);

        MaintenanceResponse result = maintenanceService.update(id, request);

        assertThat(result).isEqualTo(expected);
        assertThat(record.getVehicle()).isEqualTo(vehicle);
        assertThat(record.getTechnician()).isEqualTo(technician);
        verify(maintenanceMapper).updateEntity(request, record);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        UpdateMaintenanceRequest request = new UpdateMaintenanceRequest(
                UUID.randomUUID(), "Brake check", null, null, MaintenanceCategory.PREVENTIVE);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceService.update(id, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("MAINTENANCE_NOT_FOUND"));

        verify(maintenanceRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletesScheduledRecord_andWritesAuditLog() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        setId(record, id);
        record.setStatus(MaintenanceStatus.SCHEDULED);

        User user = new User();
        setId(user, userId);
        user.setEmail("manager@fleetmgm.com");

        setAuthentication("manager@fleetmgm.com");
        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));
        when(userRepository.findByEmail("manager@fleetmgm.com")).thenReturn(Optional.of(user));

        maintenanceService.delete(id);

        assertThat(record.getDeletedAt()).isNotNull();
        verify(maintenanceRepository).save(record);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(AuditAction.DELETE);
        assertThat(log.getEntityType()).isEqualTo("MaintenanceRecord");
        assertThat(log.getEntityId()).isEqualTo(id.toString());
        assertThat(log.getPerformedByEmail()).isEqualTo("manager@fleetmgm.com");
        assertThat(log.getPerformedByUserId()).isEqualTo(userId);
    }

    @Test
    void delete_throwsConflict_whenNotScheduled() {
        UUID id = UUID.randomUUID();
        MaintenanceRecord record = new MaintenanceRecord();
        record.setStatus(MaintenanceStatus.IN_PROGRESS);

        when(maintenanceRepository.findById(id)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> maintenanceService.delete(id))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("MAINTENANCE_DELETE_NOT_ALLOWED"));

        verify(maintenanceRepository, never()).save(any());
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

    private MaintenanceResponse buildResponse(UUID id) {
        return new MaintenanceResponse(id, UUID.randomUUID(), "1234-ABC", null, null, "Oil change", null,
                null, null, null, null, null, null, null, MaintenanceStatus.SCHEDULED,
                MaintenanceCategory.PREVENTIVE, null);
    }
}

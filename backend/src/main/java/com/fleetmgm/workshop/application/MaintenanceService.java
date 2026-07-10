package com.fleetmgm.workshop.application;

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
import com.fleetmgm.workshop.domain.MaintenanceCancelledEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final WorkerRepository workerRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public MaintenanceService(MaintenanceRepository maintenanceRepository,
                              VehicleRepository vehicleRepository,
                              WorkerRepository workerRepository,
                              MaintenanceMapper maintenanceMapper,
                              ApplicationEventPublisher eventPublisher,
                              AuditLogRepository auditLogRepository,
                              UserRepository userRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.vehicleRepository = vehicleRepository;
        this.workerRepository = workerRepository;
        this.maintenanceMapper = maintenanceMapper;
        this.eventPublisher = eventPublisher;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public PageResponse<MaintenanceResponse> list(Pageable pageable) {
        return PageResponse.from(maintenanceRepository.findAllJoinFetch(pageable).map(maintenanceMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse create(CreateMaintenanceRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker technician = resolveTechnician(request.technicianId());
        MaintenanceRecord record = maintenanceMapper.toEntity(request);
        record.setVehicle(vehicle);
        record.setTechnician(technician);
        record.setStatus(MaintenanceStatus.SCHEDULED);
        record.setCategory(request.category() != null ? request.category() : MaintenanceCategory.PREVENTIVE);
        return maintenanceMapper.toResponse(maintenanceRepository.save(record));
    }

    private Worker resolveTechnician(UUID technicianId) {
        if (technicianId == null) {
            return null;
        }
        return workerRepository.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND",
                        "Worker " + technicianId + " not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse getById(UUID id) {
        return maintenanceRepository.findById(id)
                .map(maintenanceMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse update(UUID id, UpdateMaintenanceRequest request) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker technician = resolveTechnician(request.technicianId());
        maintenanceMapper.updateEntity(request, record);
        record.setVehicle(vehicle);
        record.setTechnician(technician);
        return maintenanceMapper.toResponse(maintenanceRepository.save(record));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public void delete(UUID id) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
        if (record.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new ConflictException("MAINTENANCE_DELETE_NOT_ALLOWED",
                    "Maintenance " + id + " cannot be deleted from state " + record.getStatus());
        }
        record.setDeletedAt(Instant.now());
        maintenanceRepository.save(record);
        auditDeletion(record);
    }

    private void auditDeletion(MaintenanceRecord record) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog log = new AuditLog();
        log.setEntityType("MaintenanceRecord");
        log.setEntityId(record.getId().toString());
        log.setAction(AuditAction.DELETE);
        log.setPerformedByEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> log.setPerformedByUserId(user.getId()));
        log.setPerformedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse start(UUID id, StartMaintenanceRequest request) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
        if (record.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION",
                    "Maintenance " + id + " cannot be started from state " + record.getStatus());
        }
        record.setStatus(MaintenanceStatus.IN_PROGRESS);
        record.setWorkshopEntryDate(LocalDate.now());
        if (request != null && request.usageAtService() != null) {
            record.setUsageAtService(request.usageAtService());
        }
        MaintenanceRecord saved = maintenanceRepository.save(record);
        eventPublisher.publishEvent(new VehicleEntersWorkshopEvent(
                saved.getId(), saved.getVehicle().getId(), Instant.now()));
        return maintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse complete(UUID id, CompleteMaintenanceRequest request) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
        if (record.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION",
                    "Maintenance " + id + " cannot be completed from state " + record.getStatus());
        }
        record.setStatus(MaintenanceStatus.COMPLETED);
        record.setWorkshopExitDate(LocalDate.now());
        if (request != null && request.cost() != null) {
            record.setCost(request.cost());
        }
        MaintenanceRecord saved = maintenanceRepository.save(record);
        eventPublisher.publishEvent(new MaintenanceCompletedEvent(
                saved.getId(), saved.getVehicle().getId(), Instant.now()));
        return maintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')")
    public MaintenanceResponse cancel(UUID id) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + id + " not found"));
        if (record.getStatus() != MaintenanceStatus.SCHEDULED && record.getStatus() != MaintenanceStatus.IN_PROGRESS) {
            throw new ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION",
                    "Maintenance " + id + " cannot be cancelled from state " + record.getStatus());
        }
        record.setStatus(MaintenanceStatus.CANCELLED);
        MaintenanceRecord saved = maintenanceRepository.save(record);
        // Published unconditionally (SCHEDULED or IN_PROGRESS): ScheduleCancellationListener relies on
        // this event to cascade-cancel the linked WorkshopSchedule regardless of the maintenance's prior
        // state. MaintenanceEventListener's vehicle-reactivation guard only touches the vehicle if it is
        // actually MAINTENANCE, so cancelling a SCHEDULED record (which never moved the vehicle there)
        // is a correct no-op on the vehicle side even though the event fires.
        eventPublisher.publishEvent(new MaintenanceCancelledEvent(
                saved.getId(), saved.getVehicle().getId(), Instant.now()));
        return maintenanceMapper.toResponse(saved);
    }
}

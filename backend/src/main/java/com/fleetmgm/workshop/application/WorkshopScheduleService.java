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
import com.fleetmgm.workshop.domain.MaintenanceRecord;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Service
public class WorkshopScheduleService {

    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF')";

    private final WorkshopScheduleRepository workshopScheduleRepository;
    private final VehicleRepository vehicleRepository;
    private final WorkerRepository workerRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ScheduleMapper scheduleMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WorkshopScheduleService(WorkshopScheduleRepository workshopScheduleRepository,
                                   VehicleRepository vehicleRepository,
                                   WorkerRepository workerRepository,
                                   MaintenanceRepository maintenanceRepository,
                                   ScheduleMapper scheduleMapper,
                                   AuditLogRepository auditLogRepository,
                                   UserRepository userRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.workshopScheduleRepository = workshopScheduleRepository;
        this.vehicleRepository = vehicleRepository;
        this.workerRepository = workerRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.scheduleMapper = scheduleMapper;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<ScheduleResponse> listByRange(ScheduleRange range, Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate from = switch (range) {
            case TODAY -> today;
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
        };
        LocalDate to = switch (range) {
            case TODAY -> today;
            case WEEK -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTH -> today.withDayOfMonth(today.lengthOfMonth());
        };
        return PageResponse.from(
                workshopScheduleRepository.findAllByScheduledDateBetween(from, to, pageable)
                        .map(scheduleMapper::toResponse));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public ScheduleResponse create(CreateScheduleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker technician = resolveTechnician(request.technicianId());
        MaintenanceRecord maintenanceRecord = resolveMaintenanceRecord(request.maintenanceRecordId());
        WorkshopSchedule schedule = scheduleMapper.toEntity(request);
        schedule.setVehicle(vehicle);
        schedule.setTechnician(technician);
        schedule.setMaintenanceRecord(maintenanceRecord);
        schedule.setStatus(WorkshopStatus.PENDING);
        schedule.setPriority(request.priority() != null ? request.priority() : SchedulePriority.MEDIUM);
        return scheduleMapper.toResponse(workshopScheduleRepository.save(schedule));
    }

    private Worker resolveTechnician(UUID technicianId) {
        if (technicianId == null) {
            return null;
        }
        return workerRepository.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND",
                        "Worker " + technicianId + " not found"));
    }

    private MaintenanceRecord resolveMaintenanceRecord(UUID maintenanceRecordId) {
        if (maintenanceRecordId == null) {
            return null;
        }
        return maintenanceRepository.findById(maintenanceRecordId)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_NOT_FOUND",
                        "Maintenance " + maintenanceRecordId + " not found"));
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public ScheduleResponse getById(UUID id) {
        return workshopScheduleRepository.findById(id)
                .map(scheduleMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND",
                        "Workshop schedule " + id + " not found"));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public ScheduleResponse update(UUID id, UpdateScheduleRequest request) {
        WorkshopSchedule schedule = workshopScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND",
                        "Workshop schedule " + id + " not found"));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker technician = resolveTechnician(request.technicianId());
        MaintenanceRecord maintenanceRecord = resolveMaintenanceRecord(request.maintenanceRecordId());
        scheduleMapper.updateEntity(request, schedule);
        schedule.setVehicle(vehicle);
        schedule.setTechnician(technician);
        schedule.setMaintenanceRecord(maintenanceRecord);
        return scheduleMapper.toResponse(workshopScheduleRepository.save(schedule));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public void delete(UUID id) {
        WorkshopSchedule schedule = workshopScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND",
                        "Workshop schedule " + id + " not found"));
        if (schedule.getStatus() != WorkshopStatus.PENDING) {
            throw new ConflictException("SCHEDULE_DELETE_NOT_ALLOWED",
                    "Workshop schedule " + id + " cannot be deleted from state " + schedule.getStatus());
        }
        schedule.setDeletedAt(Instant.now());
        workshopScheduleRepository.save(schedule);
        auditDeletion(schedule);
    }

    private void auditDeletion(WorkshopSchedule schedule) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AuditLog log = new AuditLog();
        log.setEntityType("WorkshopSchedule");
        log.setEntityId(schedule.getId().toString());
        log.setAction(AuditAction.DELETE);
        log.setPerformedByEmail(email);
        userRepository.findByEmail(email).ifPresent(user -> log.setPerformedByUserId(user.getId()));
        log.setPerformedAt(Instant.now());
        auditLogRepository.save(log);
    }

    @Transactional
    @PreAuthorize(ROLES)
    public ScheduleResponse start(UUID id) {
        WorkshopSchedule schedule = workshopScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND",
                        "Workshop schedule " + id + " not found"));
        if (schedule.getStatus() != WorkshopStatus.PENDING) {
            throw new ConflictException("SCHEDULE_INVALID_STATE_TRANSITION",
                    "Workshop schedule " + id + " cannot be started from state " + schedule.getStatus());
        }
        schedule.setStatus(WorkshopStatus.IN_PROGRESS);
        return scheduleMapper.toResponse(workshopScheduleRepository.save(schedule));
    }

    @Transactional
    @PreAuthorize(ROLES)
    public ScheduleResponse cancel(UUID id) {
        WorkshopSchedule schedule = workshopScheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SCHEDULE_NOT_FOUND",
                        "Workshop schedule " + id + " not found"));
        if (schedule.getStatus() != WorkshopStatus.PENDING && schedule.getStatus() != WorkshopStatus.IN_PROGRESS) {
            throw new ConflictException("SCHEDULE_INVALID_STATE_TRANSITION",
                    "Workshop schedule " + id + " cannot be cancelled from state " + schedule.getStatus());
        }
        schedule.setStatus(WorkshopStatus.CANCELLED);
        WorkshopSchedule saved = workshopScheduleRepository.save(schedule);
        // Published unconditionally, even when maintenanceRecordId is null (unplanned-breakdown flow with
        // no linked MaintenanceRecord yet) — ScheduleCancellationListener no-ops on a null id, matching
        // ScheduleCompletionListener's existing no-op pattern for the symmetric completion event.
        UUID maintenanceRecordId = saved.getMaintenanceRecord() != null ? saved.getMaintenanceRecord().getId() : null;
        eventPublisher.publishEvent(new ScheduleCancelledEvent(saved.getId(), maintenanceRecordId, Instant.now()));
        return scheduleMapper.toResponse(saved);
    }
}

package com.fleetmgm.vehicle.application;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.dto.AssignmentMapper;
import com.fleetmgm.vehicle.dto.AssignmentResponse;
import com.fleetmgm.vehicle.dto.CreateAssignmentRequest;
import com.fleetmgm.vehicle.infrastructure.AssignmentRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final WorkerRepository workerRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AssignmentMapper assignmentMapper;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             WorkerRepository workerRepository,
                             VehicleRepository vehicleRepository,
                             UserRepository userRepository,
                             AssignmentMapper assignmentMapper) {
        this.assignmentRepository = assignmentRepository;
        this.workerRepository = workerRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.assignmentMapper = assignmentMapper;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public AssignmentResponse assign(CreateAssignmentRequest request) {
        Worker driver = workerRepository.findById(request.driverId())
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND",
                        "Worker " + request.driverId() + " not found"));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        if (assignmentRepository.findActiveByDriverId(request.driverId()).isPresent()) {
            throw new ConflictException("ASSIGNMENT_DRIVER_ALREADY_ACTIVE",
                    "Driver " + request.driverId() + " already has an active assignment");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var assignedByUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User " + email + " not found"));

        DriverVehicleAssignment assignment = assignmentMapper.toEntity(request);
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setAssignedByUser(assignedByUser);
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public AssignmentResponse endAssignment(UUID id) {
        DriverVehicleAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ASSIGNMENT_NOT_FOUND", "Assignment " + id + " not found"));
        assignment.setEndDate(LocalDate.now());
        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public PageResponse<AssignmentResponse> historyByWorker(UUID workerId, Pageable pageable) {
        return PageResponse.from(assignmentRepository.findByDriverId(workerId, pageable).map(assignmentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public Optional<AssignmentResponse> activeByVehicle(UUID vehicleId) {
        return assignmentRepository.findActiveByVehicleId(vehicleId).map(assignmentMapper::toResponse);
    }
}

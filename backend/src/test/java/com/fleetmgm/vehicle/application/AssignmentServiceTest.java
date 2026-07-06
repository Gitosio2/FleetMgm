package com.fleetmgm.vehicle.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock WorkerRepository workerRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock UserRepository userRepository;
    @Mock AssignmentMapper assignmentMapper;
    @InjectMocks AssignmentService assignmentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- assign ---

    @Test
    void assign_persistsAndReturnsDto_whenValid() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest(driverId, vehicleId, LocalDate.now(), "test");

        Worker driver = new Worker();
        Vehicle vehicle = new Vehicle();
        User currentUser = new User();
        DriverVehicleAssignment entity = new DriverVehicleAssignment();
        AssignmentResponse expected = buildAssignmentResponse(UUID.randomUUID());

        when(workerRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(assignmentRepository.findActiveByDriverId(driverId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(currentUser));
        when(assignmentMapper.toEntity(request)).thenReturn(entity);
        when(assignmentRepository.save(entity)).thenReturn(entity);
        when(assignmentMapper.toResponse(entity)).thenReturn(expected);

        AssignmentResponse result = assignmentService.assign(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<DriverVehicleAssignment> captor = ArgumentCaptor.forClass(DriverVehicleAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDriver()).isEqualTo(driver);
        assertThat(captor.getValue().getVehicle()).isEqualTo(vehicle);
        assertThat(captor.getValue().getAssignedByUser()).isEqualTo(currentUser);
    }

    @Test
    void assign_throwsConflict_whenDriverAlreadyHasActiveAssignment() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest(driverId, vehicleId, LocalDate.now(), null);

        when(workerRepository.findById(driverId)).thenReturn(Optional.of(new Worker()));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(assignmentRepository.findActiveByDriverId(driverId)).thenReturn(Optional.of(new DriverVehicleAssignment()));

        assertThatThrownBy(() -> assignmentService.assign(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("ASSIGNMENT_DRIVER_ALREADY_ACTIVE"));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assign_throwsNotFound_whenDriverMissing() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest(driverId, vehicleId, LocalDate.now(), null);

        when(workerRepository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.assign(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                        .isEqualTo("WORKER_NOT_FOUND"));
    }

    @Test
    void assign_throwsNotFound_whenVehicleMissing() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        CreateAssignmentRequest request = new CreateAssignmentRequest(driverId, vehicleId, LocalDate.now(), null);

        when(workerRepository.findById(driverId)).thenReturn(Optional.of(new Worker()));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.assign(request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                        .isEqualTo("VEHICLE_NOT_FOUND"));
    }

    // --- endAssignment ---

    @Test
    void endAssignment_setsEndDateAndReturnsDto_whenExists() {
        UUID id = UUID.randomUUID();
        DriverVehicleAssignment entity = new DriverVehicleAssignment();
        AssignmentResponse expected = buildAssignmentResponse(id);

        when(assignmentRepository.findById(id)).thenReturn(Optional.of(entity));
        when(assignmentRepository.save(entity)).thenReturn(entity);
        when(assignmentMapper.toResponse(entity)).thenReturn(expected);

        AssignmentResponse result = assignmentService.endAssignment(id);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<DriverVehicleAssignment> captor = ArgumentCaptor.forClass(DriverVehicleAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isNotNull();
    }

    @Test
    void endAssignment_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(assignmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.endAssignment(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                        .isEqualTo("ASSIGNMENT_NOT_FOUND"));
    }

    // --- helpers ---

    private AssignmentResponse buildAssignmentResponse(UUID id) {
        return new AssignmentResponse(id, UUID.randomUUID(), "Juan García", UUID.randomUUID(), "1234ABC",
                LocalDate.now(), null, UUID.randomUUID(), "notes", Instant.now(), true);
    }
}

package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.dto.CreateVehicleRequest;
import com.fleetmgm.vehicle.dto.UpdateVehicleRequest;
import com.fleetmgm.vehicle.dto.VehicleMapper;
import com.fleetmgm.vehicle.dto.VehicleResponse;
import com.fleetmgm.vehicle.infrastructure.AssignmentRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AssignmentRepository assignmentRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(VehicleRepository vehicleRepository,
                          AssignmentRepository assignmentRepository,
                          VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.assignmentRepository = assignmentRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF', 'DRIVER')")
    public PageResponse<VehicleResponse> list(Pageable pageable) {
        if (isCurrentUserDriver()) {
            return listForCurrentDriver();
        }
        return PageResponse.from(vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public VehicleResponse create(CreateVehicleRequest request) {
        if (request.licensePlate() != null && !request.licensePlate().isBlank()
                && vehicleRepository.existsByLicensePlate(request.licensePlate())) {
            throw new ConflictException("VEHICLE_LICENSE_PLATE_CONFLICT",
                    "License plate " + request.licensePlate() + " already in use");
        }
        var vehicle = vehicleMapper.toEntity(request);
        try {
            return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("VEHICLE_LICENSE_PLATE_CONFLICT",
                    "License plate " + request.licensePlate() + " already in use");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'WORKSHOP_STAFF', 'DRIVER')")
    public VehicleResponse getById(UUID id) {
        if (isCurrentUserDriver()) {
            assertDriverOwnsVehicle(id);
        }
        return vehicleRepository.findById(id)
                .map(vehicleMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
        if (request.licensePlate() != null && !request.licensePlate().isBlank()
                && vehicleRepository.existsByLicensePlateAndIdNot(request.licensePlate(), id)) {
            throw new ConflictException("VEHICLE_LICENSE_PLATE_CONFLICT",
                    "License plate " + request.licensePlate() + " already in use");
        }
        vehicleMapper.updateEntity(request, vehicle);
        try {
            return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("VEHICLE_LICENSE_PLATE_CONFLICT",
                    "License plate " + request.licensePlate() + " already in use");
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        var vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
        vehicle.setDeletedAt(Instant.now());
        vehicleRepository.save(vehicle);
    }

    // --- driver helpers ---

    private boolean isCurrentUserDriver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
    }

    private PageResponse<VehicleResponse> listForCurrentDriver() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return assignmentRepository.findActiveByDriverEmail(email)
                .map(a -> {
                    VehicleResponse v = vehicleMapper.toResponse(a.getVehicle());
                    return new PageResponse<>(List.of(v), 0, 1, 1L, 1);
                })
                .orElseGet(() -> new PageResponse<>(List.of(), 0, 1, 0L, 0));
    }

    private void assertDriverOwnsVehicle(UUID vehicleId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean owns = assignmentRepository.findActiveByDriverEmail(email)
                .map(a -> a.getVehicle().getId().equals(vehicleId))
                .orElse(false);
        if (!owns) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Driver does not have access to vehicle " + vehicleId);
        }
    }
}

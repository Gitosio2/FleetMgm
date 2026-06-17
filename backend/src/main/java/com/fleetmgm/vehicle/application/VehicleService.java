package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.dto.CreateVehicleRequest;
import com.fleetmgm.vehicle.dto.UpdateVehicleRequest;
import com.fleetmgm.vehicle.dto.VehicleMapper;
import com.fleetmgm.vehicle.dto.VehicleResponse;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> list(Pageable pageable) {
        return PageResponse.from(vehicleRepository.findAll(pageable).map(vehicleMapper::toResponse));
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        Vehicle vehicle = vehicleMapper.toEntity(request);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID id) {
        return vehicleRepository.findById(id)
                .map(vehicleMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
    }

    @Transactional
    public VehicleResponse update(UUID id, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
        vehicleMapper.updateEntity(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + id + " not found"));
        vehicle.setDeletedAt(Instant.now());
        vehicleRepository.save(vehicle);
    }
}

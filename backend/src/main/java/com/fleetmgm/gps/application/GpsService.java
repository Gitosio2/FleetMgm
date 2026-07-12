package com.fleetmgm.gps.application;

import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GpsService {

    private final GpsRepository gpsRepository;
    private final GpsMapper gpsMapper;

    public GpsService(GpsRepository gpsRepository, GpsMapper gpsMapper) {
        this.gpsRepository = gpsRepository;
        this.gpsMapper = gpsMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public List<GpsPositionResponse> findLatest(VehicleCategory category, UUID vehicleId) {
        return gpsRepository.findLatestForAllActiveVehicles().stream()
                .map(gpsMapper::toResponse)
                .filter(response -> category == null || response.vehicleCategory() == category)
                .filter(response -> vehicleId == null || response.vehicleId().equals(vehicleId))
                .toList();
    }
}

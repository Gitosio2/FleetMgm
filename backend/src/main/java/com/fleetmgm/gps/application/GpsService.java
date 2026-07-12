package com.fleetmgm.gps.application;

import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<GpsPositionResponse> findLatest() {
        return gpsRepository.findLatestForAllActiveVehicles().stream()
                .map(gpsMapper::toResponse)
                .toList();
    }
}

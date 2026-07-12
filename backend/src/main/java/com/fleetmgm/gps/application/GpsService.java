package com.fleetmgm.gps.application;

import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.infrastructure.AssignmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GpsService {

    private final GpsRepository gpsRepository;
    private final AssignmentRepository assignmentRepository;
    private final GpsMapper gpsMapper;

    public GpsService(GpsRepository gpsRepository, AssignmentRepository assignmentRepository, GpsMapper gpsMapper) {
        this.gpsRepository = gpsRepository;
        this.assignmentRepository = assignmentRepository;
        this.gpsMapper = gpsMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public List<GpsPositionResponse> findLatest() {
        if (isCurrentUserDriver()) {
            return findLatestForCurrentDriver();
        }
        return gpsRepository.findLatestForAllActiveVehicles().stream()
                .map(gpsMapper::toResponse)
                .toList();
    }

    private boolean isCurrentUserDriver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
    }

    private List<GpsPositionResponse> findLatestForCurrentDriver() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return assignmentRepository.findActiveByDriverEmail(email)
                .flatMap(a -> gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(a.getVehicle().getId()))
                .map(gpsMapper::toResponse)
                .map(List::of)
                .orElseGet(List::of);
    }
}

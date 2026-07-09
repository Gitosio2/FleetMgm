package com.fleetmgm.workshop.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.workshop.dto.CompleteMaintenanceRequest;
import com.fleetmgm.workshop.dto.CreateMaintenanceRequest;
import com.fleetmgm.workshop.dto.MaintenanceResponse;
import com.fleetmgm.workshop.dto.StartMaintenanceRequest;
import com.fleetmgm.workshop.dto.UpdateMaintenanceRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MaintenanceService {

    public PageResponse<MaintenanceResponse> list(Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public MaintenanceResponse create(CreateMaintenanceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public MaintenanceResponse getById(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public MaintenanceResponse update(UUID id, UpdateMaintenanceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public void delete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public MaintenanceResponse start(UUID id, StartMaintenanceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }

    public MaintenanceResponse complete(UUID id, CompleteMaintenanceRequest request) {
        throw new UnsupportedOperationException("Pending Hito 24");
    }
}

package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.vehicle.dto.AssignmentResponse;
import com.fleetmgm.vehicle.dto.CreateAssignmentRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AssignmentService {

    public AssignmentResponse assign(CreateAssignmentRequest request) {
        throw new UnsupportedOperationException("Pending Hito 18");
    }

    public AssignmentResponse endAssignment(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 18");
    }

    public PageResponse<AssignmentResponse> historyByWorker(UUID workerId, Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 18");
    }
}

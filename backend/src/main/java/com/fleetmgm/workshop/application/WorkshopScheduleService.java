package com.fleetmgm.workshop.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.workshop.domain.ScheduleRange;
import com.fleetmgm.workshop.dto.CreateScheduleRequest;
import com.fleetmgm.workshop.dto.ScheduleResponse;
import com.fleetmgm.workshop.dto.UpdateScheduleRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkshopScheduleService {

    public PageResponse<ScheduleResponse> listByRange(ScheduleRange range, Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public ScheduleResponse create(CreateScheduleRequest request) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public ScheduleResponse getById(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public ScheduleResponse update(UUID id, UpdateScheduleRequest request) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public void delete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public ScheduleResponse start(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }

    public ScheduleResponse cancel(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 26");
    }
}

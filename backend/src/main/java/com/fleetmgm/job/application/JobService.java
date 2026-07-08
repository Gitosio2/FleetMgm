package com.fleetmgm.job.application;

import com.fleetmgm.job.dto.CreateJobRequest;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.job.dto.UpdateJobRequest;
import com.fleetmgm.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    public PageResponse<JobResponse> list(Pageable pageable) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse create(CreateJobRequest request) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse getById(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse update(UUID id, UpdateJobRequest request) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public void delete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse start(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse complete(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }

    public JobResponse cancel(UUID id) {
        throw new UnsupportedOperationException("Pending Hito 21");
    }
}

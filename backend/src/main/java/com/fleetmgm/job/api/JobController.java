package com.fleetmgm.job.api;

import com.fleetmgm.job.application.JobService;
import com.fleetmgm.job.dto.CompleteJobRequest;
import com.fleetmgm.job.dto.CreateJobRequest;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.job.dto.StartJobRequest;
import com.fleetmgm.job.dto.UpdateJobRequest;
import com.fleetmgm.shared.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(jobService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody CreateJobRequest request,
            UriComponentsBuilder uriBuilder) {
        JobResponse response = jobService.create(request);
        URI location = uriBuilder.path("/api/v1/jobs/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(jobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<JobResponse> start(
            @PathVariable UUID id,
            @RequestBody(required = false) StartJobRequest request) {
        Long startUsageValue = request == null ? null : request.startUsageValue();
        return ResponseEntity.ok(jobService.start(id, startUsageValue));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<JobResponse> complete(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteJobRequest request) {
        Long endUsageValue = request == null ? null : request.endUsageValue();
        return ResponseEntity.ok(jobService.complete(id, endUsageValue));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<JobResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.cancel(id));
    }
}

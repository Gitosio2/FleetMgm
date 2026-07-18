package com.fleetmgm.worker.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.worker.application.WorkerService;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.worker.dto.CreateWorkerRequest;
import com.fleetmgm.worker.dto.UpdateWorkerRequest;
import com.fleetmgm.worker.dto.WorkerResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkerResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String nationalId,
            @RequestParam(required = false) WorkerRole workerRole,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(workerService.list(name, nationalId, workerRole, pageable));
    }

    @PostMapping
    public ResponseEntity<WorkerResponse> create(
            @Valid @RequestBody CreateWorkerRequest request,
            UriComponentsBuilder uriBuilder) {
        WorkerResponse response = workerService.create(request);
        URI location = uriBuilder.path("/api/v1/workers/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workerService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkerRequest request) {
        return ResponseEntity.ok(workerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

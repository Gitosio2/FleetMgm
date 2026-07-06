package com.fleetmgm.vehicle.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.vehicle.application.AssignmentService;
import com.fleetmgm.vehicle.dto.AssignmentResponse;
import com.fleetmgm.vehicle.dto.CreateAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/api/v1/assignments")
    public ResponseEntity<AssignmentResponse> assign(
            @Valid @RequestBody CreateAssignmentRequest request,
            UriComponentsBuilder uriBuilder) {
        AssignmentResponse response = assignmentService.assign(request);
        URI location = uriBuilder.path("/api/v1/assignments/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/api/v1/assignments/{id}/end")
    public ResponseEntity<AssignmentResponse> endAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.endAssignment(id));
    }

    @GetMapping("/api/v1/workers/{workerId}/assignments")
    public ResponseEntity<PageResponse<AssignmentResponse>> historyByWorker(
            @PathVariable UUID workerId,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable) {
        return ResponseEntity.ok(assignmentService.historyByWorker(workerId, pageable));
    }
}

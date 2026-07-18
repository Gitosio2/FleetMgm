package com.fleetmgm.workshop.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.workshop.application.WorkshopScheduleService;
import com.fleetmgm.workshop.domain.ScheduleRange;
import com.fleetmgm.workshop.dto.CreateScheduleRequest;
import com.fleetmgm.workshop.dto.ScheduleResponse;
import com.fleetmgm.workshop.dto.UpdateScheduleRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workshop/schedules")
public class WorkshopController {

    private final WorkshopScheduleService workshopScheduleService;

    public WorkshopController(WorkshopScheduleService workshopScheduleService) {
        this.workshopScheduleService = workshopScheduleService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ScheduleResponse>> list(
            @RequestParam(required = false) String range,
            @PageableDefault(size = 20, sort = "scheduledDate", direction = Sort.Direction.DESC) Pageable pageable) {
        ScheduleRange parsedRange = ScheduleRange.fromValue(range);
        return ResponseEntity.ok(workshopScheduleService.listByRange(parsedRange, pageable));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody CreateScheduleRequest request,
            UriComponentsBuilder uriBuilder) {
        ScheduleResponse response = workshopScheduleService.create(request);
        URI location = uriBuilder.path("/api/v1/workshop/schedules/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workshopScheduleService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScheduleRequest request) {
        return ResponseEntity.ok(workshopScheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workshopScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<ScheduleResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(workshopScheduleService.start(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ScheduleResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(workshopScheduleService.cancel(id));
    }

    // Completes the linked MaintenanceRecord (the actual work), which cascades this schedule to
    // COMPLETED via the existing ScheduleCompletionListener — no response body, see
    // WorkshopScheduleService.completeLinkedMaintenance() for why.
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable UUID id) {
        workshopScheduleService.completeLinkedMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}

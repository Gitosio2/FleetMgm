package com.fleetmgm.workshop.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.workshop.application.MaintenanceService;
import com.fleetmgm.workshop.dto.CompleteMaintenanceRequest;
import com.fleetmgm.workshop.dto.CreateMaintenanceRequest;
import com.fleetmgm.workshop.dto.MaintenanceResponse;
import com.fleetmgm.workshop.dto.StartMaintenanceRequest;
import com.fleetmgm.workshop.dto.UpdateMaintenanceRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaintenanceResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(maintenanceService.list(pageable));
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(
            @Valid @RequestBody CreateMaintenanceRequest request,
            UriComponentsBuilder uriBuilder) {
        MaintenanceResponse response = maintenanceService.create(request);
        URI location = uriBuilder.path("/api/v1/maintenance/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(maintenanceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        maintenanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<MaintenanceResponse> start(
            @PathVariable UUID id,
            @RequestBody(required = false) StartMaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.start(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<MaintenanceResponse> complete(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteMaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.complete(id, request));
    }
}

package com.fleetmgm.vehicle.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.vehicle.application.VehicleService;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.dto.CreateVehicleRequest;
import com.fleetmgm.vehicle.dto.UpdateVehicleRequest;
import com.fleetmgm.vehicle.dto.VehicleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<VehicleResponse>> list(
            @RequestParam(required = false) VehicleCategory category,
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String vehicle,
            @PageableDefault(size = 20, sort = "make") Pageable pageable) {
        return ResponseEntity.ok(vehicleService.list(category, status, licensePlate, vehicle, pageable));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(
            @Valid @RequestBody CreateVehicleRequest request,
            UriComponentsBuilder uriBuilder) {
        VehicleResponse response = vehicleService.create(request);
        URI location = uriBuilder.path("/api/v1/vehicles/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

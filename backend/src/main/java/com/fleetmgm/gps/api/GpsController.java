package com.fleetmgm.gps.api;

import com.fleetmgm.gps.application.GpsMockService;
import com.fleetmgm.gps.application.GpsService;
import com.fleetmgm.gps.dto.GpsMockStatusResponse;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.dto.UpdateGpsMockRequest;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gps")
public class GpsController {

    private final GpsService gpsService;
    private final GpsMockService gpsMockService;

    public GpsController(GpsService gpsService, GpsMockService gpsMockService) {
        this.gpsService = gpsService;
        this.gpsMockService = gpsMockService;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<GpsPositionResponse>> latest(
            @RequestParam(required = false) VehicleCategory category,
            @RequestParam(required = false) UUID vehicleId) {
        return ResponseEntity.ok(gpsService.findLatest(category, vehicleId));
    }

    // Singleton sub-resource rather than a collection — there is exactly one generator — so it has
    // no UUID in the path. PATCH per the API contract: idempotent partial update of its state.
    @GetMapping("/mock")
    public ResponseEntity<GpsMockStatusResponse> mockStatus() {
        return ResponseEntity.ok(gpsMockService.status());
    }

    @PatchMapping("/mock")
    public ResponseEntity<GpsMockStatusResponse> updateMock(@Valid @RequestBody UpdateGpsMockRequest request) {
        return ResponseEntity.ok(gpsMockService.setEnabled(request.enabled()));
    }
}

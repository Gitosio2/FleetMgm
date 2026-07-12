package com.fleetmgm.gps.api;

import com.fleetmgm.gps.application.GpsService;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gps")
public class GpsController {

    private final GpsService gpsService;

    public GpsController(GpsService gpsService) {
        this.gpsService = gpsService;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<GpsPositionResponse>> latest(
            @RequestParam(required = false) VehicleCategory category,
            @RequestParam(required = false) UUID vehicleId) {
        return ResponseEntity.ok(gpsService.findLatest(category, vehicleId));
    }
}

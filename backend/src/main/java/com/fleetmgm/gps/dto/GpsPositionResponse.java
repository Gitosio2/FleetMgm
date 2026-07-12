package com.fleetmgm.gps.dto;

import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.vehicle.domain.VehicleCategory;

import java.time.Instant;
import java.util.UUID;

public record GpsPositionResponse(
        UUID id,
        UUID vehicleId,
        String licensePlate,
        String vehicleMake,
        String vehicleModel,
        VehicleCategory vehicleCategory,
        Double latitude,
        Double longitude,
        Double heading,
        Double speed,
        Instant recordedAt,
        GpsSource source
) {}

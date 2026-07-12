package com.fleetmgm.gps.dto;

import com.fleetmgm.gps.domain.GpsSource;

import java.time.Instant;
import java.util.UUID;

public record GpsPositionResponse(
        UUID id,
        UUID vehicleId,
        String licensePlate,
        Double latitude,
        Double longitude,
        Double heading,
        Double speed,
        Instant recordedAt,
        GpsSource source
) {}

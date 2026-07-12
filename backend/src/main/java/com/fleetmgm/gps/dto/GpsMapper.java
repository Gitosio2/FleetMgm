package com.fleetmgm.gps.dto;

import com.fleetmgm.gps.domain.GpsPosition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GpsMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    GpsPositionResponse toResponse(GpsPosition gpsPosition);
}

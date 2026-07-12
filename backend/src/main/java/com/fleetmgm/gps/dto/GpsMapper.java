package com.fleetmgm.gps.dto;

import com.fleetmgm.gps.domain.GpsPosition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GpsMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "licensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "vehicleCategory", source = "vehicle.vehicleCategory")
    GpsPositionResponse toResponse(GpsPosition gpsPosition);
}

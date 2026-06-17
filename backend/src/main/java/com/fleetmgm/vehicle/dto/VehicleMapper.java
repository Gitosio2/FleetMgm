package com.fleetmgm.vehicle.dto;

import com.fleetmgm.vehicle.domain.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    VehicleResponse toResponse(Vehicle vehicle);

    Vehicle toEntity(CreateVehicleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentKm", ignore = true)
    @Mapping(target = "currentHours", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateVehicleRequest request, @MappingTarget Vehicle vehicle);
}

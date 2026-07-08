package com.fleetmgm.vehicle.dto;

import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", source = "driver.fullName")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "assignedByUserId", source = "assignedByUser.id")
    AssignmentResponse toResponse(DriverVehicleAssignment assignment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    @Mapping(target = "assignedByUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    DriverVehicleAssignment toEntity(CreateAssignmentRequest request);
}

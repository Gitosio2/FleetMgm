package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.MaintenanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "technicianId", source = "technician.id")
    @Mapping(target = "technicianName", source = "technician.fullName")
    MaintenanceResponse toResponse(MaintenanceRecord maintenanceRecord);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "technician", ignore = true)
    @Mapping(target = "usageAtService", ignore = true)
    @Mapping(target = "cost", ignore = true)
    @Mapping(target = "workshopEntryDate", ignore = true)
    @Mapping(target = "workshopExitDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    MaintenanceRecord toEntity(CreateMaintenanceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "technician", ignore = true)
    @Mapping(target = "usageAtService", ignore = true)
    @Mapping(target = "cost", ignore = true)
    @Mapping(target = "workshopEntryDate", ignore = true)
    @Mapping(target = "workshopExitDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateMaintenanceRequest request, @MappingTarget MaintenanceRecord maintenanceRecord);
}

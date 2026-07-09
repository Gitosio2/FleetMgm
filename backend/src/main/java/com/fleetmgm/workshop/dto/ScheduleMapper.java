package com.fleetmgm.workshop.dto;

import com.fleetmgm.workshop.domain.WorkshopSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "technicianId", source = "technician.id")
    @Mapping(target = "technicianName", source = "technician.fullName")
    @Mapping(target = "maintenanceRecordId", source = "maintenanceRecord.id")
    @Mapping(target = "maintenanceCategory", source = "maintenanceRecord.category")
    ScheduleResponse toResponse(WorkshopSchedule workshopSchedule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "technician", ignore = true)
    @Mapping(target = "maintenanceRecord", ignore = true)
    @Mapping(target = "priority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    WorkshopSchedule toEntity(CreateScheduleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "technician", ignore = true)
    @Mapping(target = "maintenanceRecord", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateScheduleRequest request, @MappingTarget WorkshopSchedule workshopSchedule);
}

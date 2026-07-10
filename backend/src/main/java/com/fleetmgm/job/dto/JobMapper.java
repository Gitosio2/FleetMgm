package com.fleetmgm.job.dto;

import com.fleetmgm.job.domain.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "assignedDriverId", source = "assignedDriver.id")
    @Mapping(target = "assignedDriverName", source = "assignedDriver.fullName")
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    JobResponse toResponse(Job job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "assignedDriver", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "actualStart", ignore = true)
    @Mapping(target = "actualEnd", ignore = true)
    @Mapping(target = "startUsageValue", ignore = true)
    @Mapping(target = "endUsageValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Job toEntity(CreateJobRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "assignedDriver", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "actualStart", ignore = true)
    @Mapping(target = "actualEnd", ignore = true)
    @Mapping(target = "startUsageValue", ignore = true)
    @Mapping(target = "endUsageValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateJobRequest request, @MappingTarget Job job);
}

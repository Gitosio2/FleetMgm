package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplierInvoiceMapper {

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    SupplierInvoiceResponse toResponse(SupplierInvoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    SupplierInvoice toEntity(CreateSupplierInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateSupplierInvoiceRequest request, @MappingTarget SupplierInvoice invoice);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "maintenanceRecordId", source = "maintenanceRecord.id")
    SupplierLineItemResponse toResponse(SupplierInvoiceLineItem lineItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "maintenanceRecord", ignore = true)
    SupplierInvoiceLineItem toEntity(SupplierLineItemRequest request);
}

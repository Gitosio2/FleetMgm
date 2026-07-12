package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplierInvoiceMapper {

    // lineItems is not a property of SupplierInvoice — it's assembled by SupplierInvoiceService
    // from a separate repository call (single query for getById(), batched for list() to avoid
    // N+1) and attached after this base mapping runs. Declared ignore, per CLAUDE.md MapStruct
    // convention: never rely on MapStruct's silent unmapped-field behaviour.
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleLicensePlate", source = "vehicle.licensePlate")
    @Mapping(target = "vehicleMake", source = "vehicle.make")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    @Mapping(target = "lineItems", ignore = true)
    SupplierInvoiceResponse toResponse(SupplierInvoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    SupplierInvoice toEntity(CreateSupplierInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
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

    // subtotal now maps automatically (same field name on both sides — SupplierLineItemRequest.subtotal
    // is the user-entered total cost of the line). unitPrice is derived by the service from
    // subtotal / quantity, so it's declared ignore here rather than left to silent unmapped-field
    // behaviour, per CLAUDE.md MapStruct convention.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "unitPrice", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "maintenanceRecord", ignore = true)
    SupplierInvoiceLineItem toEntity(SupplierLineItemRequest request);
}

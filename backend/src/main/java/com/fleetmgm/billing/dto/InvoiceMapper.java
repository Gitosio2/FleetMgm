package com.fleetmgm.billing.dto;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // lineItems is not a property of Invoice — it's assembled by InvoiceService from a separate
    // repository call (single query for getById(), batched for list() to avoid N+1) and attached
    // after this base mapping runs. Declared ignore, per CLAUDE.md MapStruct convention: never
    // rely on MapStruct's silent unmapped-field behaviour.
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "lineItems", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "taxRate", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Invoice toEntity(CreateInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "issueDate", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "taxRate", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateInvoiceRequest request, @MappingTarget Invoice invoice);

    @Mapping(target = "linkedJobId", source = "linkedJob.id")
    LineItemResponse toResponse(InvoiceLineItem lineItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "linkedJob", ignore = true)
    InvoiceLineItem toEntity(LineItemRequest request);
}

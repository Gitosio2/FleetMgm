package com.fleetmgm.supplier.dto;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String taxId,
        String email,
        String phone,
        String address,
        Instant createdAt
) {}

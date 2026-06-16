package com.fleetmgm.client.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String taxId,
        String email,
        String phone,
        String address,
        Instant createdAt
) {}

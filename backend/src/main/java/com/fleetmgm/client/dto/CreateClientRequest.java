package com.fleetmgm.client.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateClientRequest(
        @NotBlank String name,
        @NotBlank String taxId,
        String email,
        String phone,
        String address
) {}

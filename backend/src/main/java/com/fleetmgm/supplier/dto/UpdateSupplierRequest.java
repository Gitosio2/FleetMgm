package com.fleetmgm.supplier.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSupplierRequest(
        @NotBlank String name,
        String taxId,
        String email,
        String phone,
        String address
) {}

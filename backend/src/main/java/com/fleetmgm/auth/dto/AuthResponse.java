package com.fleetmgm.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role
) {}

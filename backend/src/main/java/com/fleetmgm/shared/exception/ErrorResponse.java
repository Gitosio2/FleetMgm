package com.fleetmgm.shared.exception;

public record ErrorResponse(
        int status,
        String code,
        String message,
        String correlationId
) {}

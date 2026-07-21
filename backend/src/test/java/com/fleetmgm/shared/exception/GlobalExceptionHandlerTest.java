package com.fleetmgm.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // A unique-constraint violation (e.g. two concurrent requests both pass a service-layer
    // uniqueness pre-check, then both insert — supplier taxId, active driver/vehicle assignment)
    // is a business-logic conflict, not a server fault — must map to a structured 409, never fall
    // through to handleGeneral()'s unhandled 500 (CLAUDE.md rule N).
    @Test
    void handleDataIntegrityViolation_returnsStructured409() {
        var ex = new DataIntegrityViolationException("duplicate key value violates unique constraint");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_CONFLICT");
        assertThat(response.getBody().correlationId()).isNotBlank();
    }
}

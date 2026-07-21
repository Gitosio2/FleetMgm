package com.fleetmgm.shared.exception;

import com.fleetmgm.shared.infrastructure.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "INVALID_CREDENTIALS", ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "VALIDATION_ERROR", message, correlationId()));
    }

    // Query-param conversion failures (e.g. an invalid enum value on ?action=...) are client input
    // errors, not unexpected server faults — without this handler they fall through to
    // handleGeneral() and surface as a misleading 500 (CLAUDE.md rule N: exceptional conditions must
    // map to a structured response, not an unhandled fault).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "VALIDATION_ERROR", message, correlationId()));
    }

    // A stale-write conflict (two concurrent requests both loaded the same @Version'd entity before
    // either committed) is a business-logic condition, not a server fault — must map to a structured
    // 409, never fall through to handleGeneral()'s unhandled 500 (CLAUDE.md rule N).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "CONCURRENT_MODIFICATION",
                        "The resource was modified by another request. Reload and try again.", correlationId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "ACCESS_DENIED", "Access denied", correlationId()));
    }

    // A unique-constraint violation reaching the DB (e.g. two concurrent requests both pass a
    // service-layer uniqueness pre-check — supplier taxId, active driver/vehicle assignment — then
    // both insert) is a business-logic conflict, not a server fault. Without this handler it falls
    // through to handleGeneral() and surfaces as an unhandled 500 (CLAUDE.md rule N: exceptional
    // conditions must map to a structured response, never a silent fail-open or a raw fault).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String id = correlationId();
        log.warn("Data integrity violation [correlationId={}]", id, ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "DATA_INTEGRITY_CONFLICT",
                        "The request conflicts with an existing resource.", id));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        String id = correlationId();
        log.error("Unhandled exception [correlationId={}]", id, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred", id));
    }

    // Falls back to a fresh random ID only if CorrelationIdFilter didn't run for this request
    // (e.g. a unit/@WebMvcTest invoking the handler directly, bypassing the filter chain) — in
    // real traffic MDC always has one, so the returned ID matches this request's log lines.
    private String correlationId() {
        String fromRequest = MDC.get(CorrelationIdFilter.MDC_KEY);
        return fromRequest != null ? fromRequest : UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}

package com.fleetmgm.shared.api;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.application.AuditLogService;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.dto.AuditLogResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String performedByEmail,
            @PageableDefault(size = 20, sort = "performedAt") Pageable pageable) {
        return ResponseEntity.ok(auditLogService.list(entityType, action, from, to, performedByEmail, pageable));
    }
}

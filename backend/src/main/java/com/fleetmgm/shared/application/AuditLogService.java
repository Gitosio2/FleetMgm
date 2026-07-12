package com.fleetmgm.shared.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLog;
import com.fleetmgm.shared.dto.AuditLogMapper;
import com.fleetmgm.shared.dto.AuditLogResponse;
import com.fleetmgm.shared.infrastructure.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditLogService {

    // Stricter than the usual ADMIN/MANAGER/ADMINISTRATIVE trio (see SupplierInvoiceService.ROLES)
    // — the audit trail itself must not be readable by the role it would be used to catch (planning.md
    // Hito 41: "solo ADMIN/MANAGER").
    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER')";

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public PageResponse<AuditLogResponse> list(String entityType, AuditAction action, Instant from, Instant to,
                                                String performedByEmail, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAllFiltered(entityType, action, from, to, performedByEmail,
                pageable);
        return PageResponse.from(page.map(auditLogMapper::toResponse));
    }
}

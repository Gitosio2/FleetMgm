package com.fleetmgm.worker.application;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import org.springframework.dao.DataIntegrityViolationException;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.worker.dto.CreateWorkerRequest;
import com.fleetmgm.worker.dto.UpdateWorkerRequest;
import com.fleetmgm.worker.dto.WorkerMapper;
import com.fleetmgm.worker.dto.WorkerResponse;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerService {

    private static final String ENTITY_TYPE = "Worker";

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final WorkerMapper workerMapper;
    private final AuditLogHelper auditLogHelper;

    public WorkerService(WorkerRepository workerRepository,
                         UserRepository userRepository,
                         WorkerMapper workerMapper,
                         AuditLogHelper auditLogHelper) {
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.workerMapper = workerMapper;
        this.auditLogHelper = auditLogHelper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public PageResponse<WorkerResponse> list(String name, String nationalId, WorkerRole workerRole, Pageable pageable) {
        if (isCurrentUserDriver()) {
            // A driver only ever sees their own profile — filters are meaningless here and are
            // intentionally not threaded into this branch.
            return listForCurrentDriver();
        }
        return PageResponse.from(
                workerRepository.search(name, nationalId, workerRole, pageable).map(workerMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public WorkerResponse create(CreateWorkerRequest request) {
        if (workerRepository.existsByNationalId(request.nationalId())) {
            throw new ConflictException("WORKER_NATIONAL_ID_CONFLICT",
                    "National ID " + request.nationalId() + " already in use");
        }
        Worker worker = workerMapper.toEntity(request);
        if (request.userId() != null) {
            userRepository.findById(request.userId()).ifPresent(worker::setUser);
        }
        try {
            var saved = workerMapper.toResponse(workerRepository.save(worker));
            auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.CREATE);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("WORKER_NATIONAL_ID_CONFLICT",
                    "National ID " + request.nationalId() + " already in use");
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public WorkerResponse getById(UUID id) {
        if (isCurrentUserDriver()) {
            assertDriverOwnsProfile(id);
        }
        return workerRepository.findById(id)
                .map(workerMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND", "Worker " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public WorkerResponse update(UUID id, UpdateWorkerRequest request) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND", "Worker " + id + " not found"));
        if (workerRepository.existsByNationalIdAndIdNot(worker.getNationalId(), id)) {
            throw new ConflictException("WORKER_NATIONAL_ID_CONFLICT",
                    "National ID " + worker.getNationalId() + " already in use");
        }
        workerMapper.updateEntity(request, worker);
        var saved = workerMapper.toResponse(workerRepository.save(worker));
        auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.UPDATE);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND", "Worker " + id + " not found"));
        worker.setDeletedAt(Instant.now());
        workerRepository.save(worker);
        auditLogHelper.log(ENTITY_TYPE, id.toString(), AuditAction.DELETE);
    }

    // --- driver helpers ---

    private boolean isCurrentUserDriver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
    }

    private PageResponse<WorkerResponse> listForCurrentDriver() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .flatMap(user -> workerRepository.findByUserId(user.getId()))
                .map(worker -> {
                    WorkerResponse r = workerMapper.toResponse(worker);
                    return new PageResponse<>(List.of(r), 0, 1, 1L, 1);
                })
                .orElseGet(() -> new PageResponse<>(List.of(), 0, 1, 0L, 0));
    }

    private void assertDriverOwnsProfile(UUID workerId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean owns = userRepository.findByEmail(email)
                .flatMap(user -> workerRepository.findByUserId(user.getId()))
                .map(w -> w.getId().equals(workerId))
                .orElse(false);
        if (!owns) {
            throw new AccessDeniedException("Driver does not have access to worker profile " + workerId);
        }
    }
}

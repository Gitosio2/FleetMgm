package com.fleetmgm.job.application;

import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.job.dto.CreateJobRequest;
import com.fleetmgm.job.dto.JobMapper;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.job.dto.UpdateJobRequest;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.BadRequestException;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
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
public class JobService {

    private final JobRepository jobRepository;
    private final VehicleRepository vehicleRepository;
    private final WorkerRepository workerRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final ApplicationEventPublisher eventPublisher;

    public JobService(JobRepository jobRepository,
                      VehicleRepository vehicleRepository,
                      WorkerRepository workerRepository,
                      ClientRepository clientRepository,
                      UserRepository userRepository,
                      JobMapper jobMapper,
                      ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.vehicleRepository = vehicleRepository;
        this.workerRepository = workerRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.jobMapper = jobMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public PageResponse<JobResponse> list(Pageable pageable) {
        // The repository query's own ORDER BY (not-started-first, then most recently started) is the
        // whole point — a caller-supplied Sort would just get appended after it, so it's stripped
        // here rather than trusted from the controller.
        Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        if (isCurrentUserDriver()) {
            return listForCurrentDriver(pageOnly);
        }
        return PageResponse.from(jobRepository.findAllJoinFetch(pageOnly).map(jobMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public JobResponse create(CreateJobRequest request) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker assignedDriver = resolveDriver(request.assignedDriverId());
        Client client = resolveClient(request.clientId());

        Job job = jobMapper.toEntity(request);
        job.setVehicle(vehicle);
        job.setAssignedDriver(assignedDriver);
        job.setClient(client);
        validateActualDates(job);
        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public JobResponse getById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        if (isCurrentUserDriver()) {
            assertDriverOwnsJob(job);
            assertJobIsActive(job);
        }
        return jobMapper.toResponse(job);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public JobResponse update(UUID id, UpdateJobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + request.vehicleId() + " not found"));
        Worker assignedDriver = resolveDriver(request.assignedDriverId());
        Client client = resolveClient(request.clientId());

        jobMapper.updateEntity(request, job);
        job.setVehicle(vehicle);
        job.setAssignedDriver(assignedDriver);
        job.setClient(client);
        validateActualDates(job);
        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        job.setDeletedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public JobResponse start(UUID id, Long startUsageValue) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        if (isCurrentUserDriver()) {
            assertDriverOwnsJob(job);
        }
        if (job.getStatus() != JobStatus.PENDING) {
            throw new ConflictException("JOB_INVALID_STATE_TRANSITION",
                    "Job " + id + " cannot be started from state " + job.getStatus());
        }
        job.setStatus(JobStatus.IN_PROGRESS);
        // A manually-set actualStart (from the create/edit form) must survive a subsequent
        // start() call — only stamp "now" when nobody has recorded one yet.
        if (job.getActualStart() == null) {
            job.setActualStart(Instant.now());
        }
        if (startUsageValue != null) {
            job.setStartUsageValue(startUsageValue);
        }
        return jobMapper.toResponse(jobRepository.save(job));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public JobResponse complete(UUID id, Long endUsageValue) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        if (isCurrentUserDriver()) {
            assertDriverOwnsJob(job);
        }
        if (job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new ConflictException("JOB_INVALID_STATE_TRANSITION",
                    "Job " + id + " cannot be completed from state " + job.getStatus());
        }
        if (endUsageValue != null) {
            assertUsageValueNotRegressing(job, endUsageValue);
            job.setEndUsageValue(endUsageValue);
        }
        job.setStatus(JobStatus.COMPLETED);
        // Same rationale as start(): don't overwrite a manually-set actualEnd.
        if (job.getActualEnd() == null) {
            job.setActualEnd(Instant.now());
        }
        Job saved = jobRepository.save(job);
        eventPublisher.publishEvent(new JobCompletedEvent(
                saved.getId(), saved.getVehicle().getId(),
                saved.getClient() != null ? saved.getClient().getId() : null,
                saved.getPrice(), saved.getTitle(),
                saved.getEndUsageValue(), saved.getActualEnd()));
        return jobMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE', 'DRIVER')")
    public JobResponse cancel(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Job " + id + " not found"));
        if (isCurrentUserDriver()) {
            assertDriverOwnsJob(job);
        }
        if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.IN_PROGRESS) {
            throw new ConflictException("JOB_INVALID_STATE_TRANSITION",
                    "Job " + id + " cannot be cancelled from state " + job.getStatus());
        }
        job.setStatus(JobStatus.CANCELLED);
        return jobMapper.toResponse(jobRepository.save(job));
    }

    // actualStart/actualEnd represent things that already happened (unlike scheduledStart/scheduledEnd,
    // which are legitimately future-facing) — a manually-set actualEnd flows straight into
    // JobCompletedEvent.completedAt() and from there into UsageLog.recordedAt with no further
    // clamping, so bad values must be rejected here at the source.
    private void validateActualDates(Job job) {
        Instant actualStart = job.getActualStart();
        Instant actualEnd = job.getActualEnd();
        if (actualEnd != null && actualStart == null) {
            throw new BadRequestException("JOB_ACTUAL_END_WITHOUT_START",
                    "actualEnd cannot be set without actualStart");
        }
        if (actualStart != null && actualEnd != null && actualEnd.isBefore(actualStart)) {
            throw new BadRequestException("JOB_ACTUAL_END_BEFORE_START",
                    "actualEnd must not be before actualStart");
        }
        Instant now = Instant.now();
        if (actualStart != null && actualStart.isAfter(now)) {
            throw new BadRequestException("JOB_ACTUAL_DATE_IN_FUTURE", "actualStart cannot be in the future");
        }
        if (actualEnd != null && actualEnd.isAfter(now)) {
            throw new BadRequestException("JOB_ACTUAL_DATE_IN_FUTURE", "actualEnd cannot be in the future");
        }
    }

    // --- relation resolution helpers ---

    private Worker resolveDriver(UUID driverId) {
        if (driverId == null) {
            return null;
        }
        return workerRepository.findById(driverId)
                .orElseThrow(() -> new NotFoundException("WORKER_NOT_FOUND", "Worker " + driverId + " not found"));
    }

    private Client resolveClient(UUID clientId) {
        if (clientId == null) {
            return null;
        }
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + clientId + " not found"));
    }

    // Usage counters (km/hours) must be monotonic — reject an endUsageValue lower than either the
    // vehicle's currently recorded value or this job's own startUsageValue (typo, stale/duplicate
    // completion, or a job that never actually advanced the odometer).
    private void assertUsageValueNotRegressing(Job job, long endUsageValue) {
        Long currentValue = job.getVehicle().getCurrentUsageValue();
        Long startValue = job.getStartUsageValue();
        long floor = Math.max(
                currentValue != null ? currentValue : Long.MIN_VALUE,
                startValue != null ? startValue : Long.MIN_VALUE);
        if (floor != Long.MIN_VALUE && endUsageValue < floor) {
            throw new ConflictException("JOB_USAGE_VALUE_BELOW_CURRENT",
                    "endUsageValue " + endUsageValue + " for job " + job.getId()
                            + " is lower than the current recorded usage (" + floor + ")");
        }
    }

    // --- driver helpers ---

    // Matches planning.md's permission matrix: DRIVER only sees/acts on their own PENDING/IN_PROGRESS jobs,
    // never past history (COMPLETED/CANCELLED) even for jobs assigned to them.
    private static final List<JobStatus> DRIVER_ACTIVE_STATUSES = List.of(JobStatus.PENDING, JobStatus.IN_PROGRESS);

    private boolean isCurrentUserDriver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_DRIVER"::equals);
    }

    private PageResponse<JobResponse> listForCurrentDriver(Pageable pageable) {
        Worker driver = currentDriverWorker();
        if (driver == null) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
        }
        return PageResponse.from(jobRepository
                .findByAssignedDriverIdAndStatusIn(driver.getId(), DRIVER_ACTIVE_STATUSES, pageable)
                .map(jobMapper::toResponse));
    }

    private Worker currentDriverWorker() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .flatMap(user -> workerRepository.findByUserId(user.getId()))
                .orElse(null);
    }

    private void assertDriverOwnsJob(Job job) {
        Worker driver = currentDriverWorker();
        boolean owns = driver != null && job.getAssignedDriver() != null
                && job.getAssignedDriver().getId().equals(driver.getId());
        if (!owns) {
            throw new AccessDeniedException("Driver does not have access to job " + job.getId());
        }
    }

    private void assertJobIsActive(Job job) {
        if (!DRIVER_ACTIVE_STATUSES.contains(job.getStatus())) {
            throw new AccessDeniedException("Driver does not have access to job history for job " + job.getId());
        }
    }
}

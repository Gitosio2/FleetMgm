package com.fleetmgm.job.infrastructure;

import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    // List query denormalizes vehicle/driver/client fields into JobResponse — JOIN FETCH avoids N+1
    // (CLAUDE.md JPA rule). Safe with Pageable: these are to-one joins, not to-many collections.
    // Jobs with no actual start (not yet started) sort first; among the rest, most recently started
    // first. Callers must pass an unsorted Pageable — this ORDER BY is authoritative.
    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client "
            + "ORDER BY CASE WHEN j.actualStart IS NULL THEN 0 ELSE 1 END ASC, j.actualStart DESC")
    Page<Job> findAllJoinFetch(Pageable pageable);

    // Same JOIN FETCH/ordering rationale as findAllJoinFetch, with optional filters layered on top —
    // same "(CAST(:param AS string) IS NULL OR ...)" JPQL idiom as WorkerRepository.search()/
    // VehicleRepository.search() (every bare-occurrence param needs the CAST, or Postgres 500s on
    // unfiltered requests — see AuditLogRepository's comment on the same idiom). Callers must pass
    // an unsorted Pageable — this ORDER BY is authoritative, same as findAllJoinFetch.
    // :vehicleId and :assignedDriverId are exact matches against the vehicle/driver UUID — both
    // frontend filters are selects populated from the real vehicle/driver lists, not free text
    // (same rationale for both). A job with no assignedDriver never matches a non-null
    // assignedDriverId filter since j.assignedDriver.id is null; j.vehicle is never null (Job.vehicle
    // is a required relation), so :vehicleId has no equivalent no-match-by-null case.
    // actualStart/actualEnd each get a from/to range (not exact-day equality) — the only existing
    // date-filter precedent in this codebase (AuditLogRepository.findAllFiltered's :from/:to on
    // performedAt) is a range, so this reuses that pattern instead of inventing a new one.
    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client "
            + "WHERE (CAST(:title AS string) IS NULL OR LOWER(j.title) LIKE "
            + "     LOWER(CONCAT('%', CAST(:title AS string), '%'))) "
            + "AND (CAST(:originLocation AS string) IS NULL OR LOWER(j.originLocation) LIKE "
            + "     LOWER(CONCAT('%', CAST(:originLocation AS string), '%'))) "
            + "AND (CAST(:destinationLocation AS string) IS NULL OR LOWER(j.destinationLocation) LIKE "
            + "     LOWER(CONCAT('%', CAST(:destinationLocation AS string), '%'))) "
            + "AND (CAST(:vehicleId AS string) IS NULL OR j.vehicle.id = :vehicleId) "
            + "AND (CAST(:assignedDriverId AS string) IS NULL OR j.assignedDriver.id = :assignedDriverId) "
            + "AND (CAST(:status AS string) IS NULL OR j.status = :status) "
            + "AND (CAST(:actualStartFrom AS string) IS NULL OR j.actualStart >= :actualStartFrom) "
            + "AND (CAST(:actualStartTo AS string) IS NULL OR j.actualStart <= :actualStartTo) "
            + "AND (CAST(:actualEndFrom AS string) IS NULL OR j.actualEnd >= :actualEndFrom) "
            + "AND (CAST(:actualEndTo AS string) IS NULL OR j.actualEnd <= :actualEndTo) "
            + "ORDER BY CASE WHEN j.actualStart IS NULL THEN 0 ELSE 1 END ASC, j.actualStart DESC")
    Page<Job> search(@Param("title") String title, @Param("originLocation") String originLocation,
            @Param("destinationLocation") String destinationLocation, @Param("vehicleId") UUID vehicleId,
            @Param("assignedDriverId") UUID assignedDriverId, @Param("status") JobStatus status,
            @Param("actualStartFrom") Instant actualStartFrom, @Param("actualStartTo") Instant actualStartTo,
            @Param("actualEndFrom") Instant actualEndFrom, @Param("actualEndTo") Instant actualEndTo,
            Pageable pageable);

    // Same ordering rationale as findAllJoinFetch above. Callers must pass an unsorted Pageable.
    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client "
            + "WHERE j.assignedDriver.id = :driverId AND j.status IN :statuses "
            + "ORDER BY CASE WHEN j.actualStart IS NULL THEN 0 ELSE 1 END ASC, j.actualStart DESC")
    Page<Job> findByAssignedDriverIdAndStatusIn(@Param("driverId") UUID driverId,
                                                 @Param("statuses") Collection<JobStatus> statuses,
                                                 Pageable pageable);
}

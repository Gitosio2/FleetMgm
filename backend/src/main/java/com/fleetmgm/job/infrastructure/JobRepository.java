package com.fleetmgm.job.infrastructure;

import com.fleetmgm.job.domain.Job;
import com.fleetmgm.job.domain.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Same ordering rationale as findAllJoinFetch above. Callers must pass an unsorted Pageable.
    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client "
            + "WHERE j.assignedDriver.id = :driverId AND j.status IN :statuses "
            + "ORDER BY CASE WHEN j.actualStart IS NULL THEN 0 ELSE 1 END ASC, j.actualStart DESC")
    Page<Job> findByAssignedDriverIdAndStatusIn(@Param("driverId") UUID driverId,
                                                 @Param("statuses") Collection<JobStatus> statuses,
                                                 Pageable pageable);
}

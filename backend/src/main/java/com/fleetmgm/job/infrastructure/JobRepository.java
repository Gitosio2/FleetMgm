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
    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client")
    Page<Job> findAllJoinFetch(Pageable pageable);

    @Query("SELECT j FROM Job j JOIN FETCH j.vehicle LEFT JOIN FETCH j.assignedDriver LEFT JOIN FETCH j.client "
            + "WHERE j.assignedDriver.id = :driverId AND j.status IN :statuses")
    Page<Job> findByAssignedDriverIdAndStatusIn(@Param("driverId") UUID driverId,
                                                 @Param("statuses") Collection<JobStatus> statuses,
                                                 Pageable pageable);
}

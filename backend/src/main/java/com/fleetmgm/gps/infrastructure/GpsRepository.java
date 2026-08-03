package com.fleetmgm.gps.infrastructure;

import com.fleetmgm.gps.domain.GpsPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GpsRepository extends JpaRepository<GpsPosition, UUID> {

    @Query("""
            SELECT g FROM GpsPosition g
            JOIN FETCH g.vehicle v
            WHERE v.status = com.fleetmgm.vehicle.domain.VehicleStatus.ACTIVE
            AND g.recordedAt = (
                SELECT MAX(g2.recordedAt) FROM GpsPosition g2 WHERE g2.vehicle = g.vehicle
            )
            """)
    List<GpsPosition> findLatestForAllActiveVehicles();

    /**
     * Bulk-deletes expired positions in a single statement. A derived {@code deleteByRecordedAtBefore}
     * would load every matching row as a managed entity and issue one DELETE each — the opposite of
     * what a retention sweep over a table with hundreds of thousands of rows needs.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GpsPosition g WHERE g.recordedAt < :cutoff")
    int deleteRecordedBefore(@Param("cutoff") Instant cutoff);
}

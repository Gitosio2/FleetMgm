package com.fleetmgm.gps.infrastructure;

import com.fleetmgm.gps.domain.GpsPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GpsRepository extends JpaRepository<GpsPosition, UUID> {

    @EntityGraph(attributePaths = "vehicle")
    Optional<GpsPosition> findFirstByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);

    @Query("""
            SELECT g FROM GpsPosition g
            JOIN FETCH g.vehicle v
            WHERE v.status = com.fleetmgm.vehicle.domain.VehicleStatus.ACTIVE
            AND g.recordedAt = (
                SELECT MAX(g2.recordedAt) FROM GpsPosition g2 WHERE g2.vehicle = g.vehicle
            )
            """)
    List<GpsPosition> findLatestForAllActiveVehicles();
}

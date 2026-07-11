package com.fleetmgm.workshop.infrastructure;

import com.fleetmgm.workshop.domain.MaintenanceRecord;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {

    // List query denormalizes vehicle/technician fields into MaintenanceResponse — JOIN FETCH
    // avoids N+1 (CLAUDE.md JPA rule). Safe with Pageable: these are to-one joins, not to-many collections.
    @Query("SELECT m FROM MaintenanceRecord m JOIN FETCH m.vehicle "
            + "LEFT JOIN FETCH m.technician")
    Page<MaintenanceRecord> findAllJoinFetch(Pageable pageable);

    // Used by MaintenanceEventListener to decide whether a completed maintenance should return the
    // vehicle to ACTIVE — it must stay in the workshop while another maintenance is still IN_PROGRESS.
    boolean existsByVehicleIdAndStatus(UUID vehicleId, MaintenanceStatus status);
}

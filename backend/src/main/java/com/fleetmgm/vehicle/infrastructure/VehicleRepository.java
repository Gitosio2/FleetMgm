package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, UUID id);

    // Soft-deleted records are excluded automatically via @SQLRestriction on Vehicle.
    // The JOIN FETCH optimization for active-assignment data will be added in Hito 12
    // when VehicleResponse includes driver info.
    @Query("SELECT v FROM Vehicle v")
    Page<Vehicle> findAllActiveWithAssignment(Pageable pageable);

    List<Vehicle> findAllByStatus(VehicleStatus status);

    // Fleet-summary KPIs (dashboard): plain derived counts, no JOIN FETCH needed since only the
    // row count is returned, not the entities themselves.
    long countByStatus(VehicleStatus status);

    long countByStatusNot(VehicleStatus status);
}

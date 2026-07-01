package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, UUID id);

    // Soft-deleted records are excluded automatically via @SQLRestriction on Vehicle.
    // The JOIN FETCH optimization for active-assignment data will be added in Hito 12
    // when VehicleResponse includes driver info.
    @Query("SELECT v FROM Vehicle v")
    Page<Vehicle> findAllActiveWithAssignment(Pageable pageable);
}

package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<DriverVehicleAssignment, UUID> {

    // Traverses: DriverVehicleAssignment -> driver (Worker) -> user (User) -> email
    // Used by VehicleService to enforce DRIVER permission without requiring WorkerRepository
    @Query("SELECT a FROM DriverVehicleAssignment a JOIN a.driver w JOIN w.user u WHERE u.email = :email AND a.endDate IS NULL")
    Optional<DriverVehicleAssignment> findActiveByDriverEmail(@Param("email") String email);

    @Query("SELECT a FROM DriverVehicleAssignment a WHERE a.driver.id = :driverId AND a.endDate IS NULL")
    Optional<DriverVehicleAssignment> findActiveByDriverId(@Param("driverId") UUID driverId);

    @Query("SELECT a FROM DriverVehicleAssignment a WHERE a.vehicle.id = :vehicleId AND a.endDate IS NULL")
    Optional<DriverVehicleAssignment> findActiveByVehicleId(@Param("vehicleId") UUID vehicleId);

    Page<DriverVehicleAssignment> findByDriverId(UUID driverId, Pageable pageable);
}

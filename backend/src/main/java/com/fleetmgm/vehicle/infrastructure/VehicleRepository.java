package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // All four filters are optional and case-insensitive partial matches — same
    // "(CAST(:param AS string) IS NULL OR ...)" JPQL idiom as SupplierRepository.search()/
    // WorkerRepository.search(). No ORDER BY here: Spring Data applies the Pageable's own Sort
    // automatically (the controller passes @PageableDefault(sort = "make")). There's no single
    // "vehicle name" column — make and model are concatenated the same way VehicleTable already
    // displays them ("Vehículo" column), so the filter matches what the UI shows.
    // :licensePlate/:vehicle need BOTH occurrences cast, not just the IS NULL one — see
    // SupplierRepository.search()'s comment: leaving the CONCAT occurrence uncast fails against
    // real Postgres ("function lower(bytea) does not exist"), because each JPQL "?" placeholder
    // binds independently and pgjdbc binds an untyped null parameter to bytea by default.
    // :category/:status are enums but get the same CAST(:param AS string) IS NULL treatment as the
    // string params, not a bare ":param IS NULL" — see AuditLogRepository's comment on the same
    // idiom: a parameter that appears ONLY in a bare IS NULL check gives Postgres no type context
    // to infer from, even for enums, and fails 500 on every request that leaves it null.
    @Query("SELECT v FROM Vehicle v "
            + "WHERE (CAST(:category AS string) IS NULL OR v.vehicleCategory = :category) "
            + "AND (CAST(:status AS string) IS NULL OR v.status = :status) "
            + "AND (CAST(:licensePlate AS string) IS NULL OR LOWER(v.licensePlate) LIKE "
            + "     LOWER(CONCAT('%', CAST(:licensePlate AS string), '%'))) "
            + "AND (CAST(:vehicle AS string) IS NULL OR LOWER(CONCAT(v.make, ' ', v.model)) LIKE "
            + "     LOWER(CONCAT('%', CAST(:vehicle AS string), '%')))")
    Page<Vehicle> search(@Param("category") VehicleCategory category, @Param("status") VehicleStatus status,
            @Param("licensePlate") String licensePlate, @Param("vehicle") String vehicle, Pageable pageable);
}

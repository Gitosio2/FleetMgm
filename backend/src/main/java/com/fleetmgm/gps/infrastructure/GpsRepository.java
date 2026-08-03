package com.fleetmgm.gps.infrastructure;

import com.fleetmgm.gps.domain.GpsPosition;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GpsRepository extends JpaRepository<GpsPosition, UUID> {

    /**
     * Latest position per active vehicle. Composed of two statements rather than one JOIN-FETCH
     * query with a correlated {@code MAX(recordedAt)} filter: at production-scale history (~20k
     * rows/vehicle even after the 7-day retention window), that single-query shape forced Postgres
     * to re-evaluate the correlated subquery once per candidate row scanned — ~560k executions for a
     * 28-vehicle fleet, ~1.6s and ~2.8M buffer hits measured locally. A composite index alone did not
     * fix it; the planner kept choosing a full-table scan over the query shape regardless.
     * <p>
     * {@link #findLatestPositionIdsForAllActiveVehicles()} asks Postgres for one indexed row per
     * vehicle via {@code LATERAL} (a single {@code idx_gps_vehicle_recorded} probe per vehicle,
     * measured at ~0.2ms for the same dataset), then {@link #findAllByIdInWithVehicle} hydrates full
     * entities — with {@code vehicle} eagerly fetched — for exactly those ids. Two statements, but
     * both O(vehicles), not O(history).
     */
    default List<GpsPosition> findLatestForAllActiveVehicles() {
        List<UUID> ids = findLatestPositionIdsForAllActiveVehicles();
        return ids.isEmpty() ? List.of() : findAllByIdInWithVehicle(ids);
    }

    /**
     * {@code LATERAL} is Postgres-specific and unavailable in JPQL, hence native SQL — but with no
     * user input in the statement (the only predicate is a hardcoded enum literal), so this doesn't
     * fall under the dynamic-native-query ban in CLAUDE.md's SQL injection rule. {@code LIMIT 1} per
     * vehicle also removes the old query's latent double-row bug on an exact {@code recordedAt} tie
     * (two positions saved in the same millisecond used to both count as "latest"); Postgres breaks
     * the tie arbitrarily but always returns exactly one row.
     */
    @Query(value = """
            SELECT gp.id
            FROM vehicles v
            JOIN LATERAL (
                SELECT gps.id
                FROM gps_positions gps
                WHERE gps.vehicle_id = v.id
                ORDER BY gps.recorded_at DESC
                LIMIT 1
            ) gp ON true
            WHERE v.status = 'ACTIVE'
            """, nativeQuery = true)
    List<UUID> findLatestPositionIdsForAllActiveVehicles();

    @Query("SELECT g FROM GpsPosition g JOIN FETCH g.vehicle WHERE g.id IN :ids")
    List<GpsPosition> findAllByIdInWithVehicle(@Param("ids") List<UUID> ids);

    /**
     * Bulk-deletes expired positions in a single statement. A derived {@code deleteByRecordedAtBefore}
     * would load every matching row as a managed entity and issue one DELETE each — the opposite of
     * what a retention sweep over a table with hundreds of thousands of rows needs.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM GpsPosition g WHERE g.recordedAt < :cutoff")
    int deleteRecordedBefore(@Param("cutoff") Instant cutoff);
}

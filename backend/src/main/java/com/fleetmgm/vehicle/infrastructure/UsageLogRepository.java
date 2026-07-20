package com.fleetmgm.vehicle.infrastructure;

import com.fleetmgm.vehicle.domain.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

    /**
     * Last known reading strictly before the start of {@code from}'s day — the period's baseline
     * for {@code ProfitabilityService.computeUsageInRange}. Native SQL: "latest reading at/before a
     * boundary" isn't expressible as a derived Spring Data method name.
     * <p>
     * {@code measureType} (the vehicle's CURRENT {@code usageMeasure}, e.g. "KILOMETERS") is a
     * required filter, not optional: {@code Vehicle.usageMeasure} is editable via
     * {@code UpdateVehicleRequest}, so a vehicle switched from KILOMETERS to HOURS (or vice versa)
     * can have {@code usage_logs} rows in both units — each row's {@code measure_type} was stamped
     * with whatever the vehicle's measure was at the time that job completed
     * ({@code JobEventListener.onJobCompleted}). Without this filter, "latest reading" could pick a
     * reading recorded under the OLD measure as the baseline and one under the NEW measure as the
     * end value, subtracting incompatible units into a nonsense delta.
     * <p>
     * {@code recorded_at} is {@code TIMESTAMPTZ} (an absolute instant). Casting {@code :from} to
     * {@code timestamp} (not {@code date}) before {@code AT TIME ZONE 'UTC'} is deliberate: bare
     * {@code date AT TIME ZONE 'UTC'} resolves to PostgreSQL's {@code timestamptz AT TIME ZONE}
     * overload, which implicitly casts the date to {@code timestamptz} using the JDBC session's
     * timezone (following the JVM default, e.g. Europe/Madrid — not necessarily UTC) *before*
     * reinterpreting in UTC, which is backwards. Casting to {@code timestamp} first forces the
     * {@code timestamp AT TIME ZONE} overload, which treats the naive midnight value as already
     * being UTC wall-clock time — the correct boundary. Verified by
     * {@code UsageLogRepositoryTest}'s day-boundary tests, which fail without this cast on a
     * non-UTC JVM.
     */
    @Query(value = "SELECT ul.value FROM usage_logs ul WHERE ul.vehicle_id = :vehicleId "
            + "AND ul.measure_type = :measureType "
            + "AND ul.recorded_at < CAST(:from AS timestamp) AT TIME ZONE 'UTC' "
            + "ORDER BY ul.recorded_at DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findLatestValueBeforeDate(@Param("vehicleId") UUID vehicleId, @Param("measureType") String measureType,
            @Param("from") LocalDate from);

    /**
     * Last known reading at/before the END of {@code to}'s day (inclusive of that whole day) — the
     * period's end value for {@code ProfitabilityService.computeUsageInRange}. See
     * {@link #findLatestValueBeforeDate} for why {@code measureType} is a required filter and why
     * the boundary is anchored via {@code AT TIME ZONE 'UTC'} instead of a bare date cast.
     */
    @Query(value = "SELECT ul.value FROM usage_logs ul WHERE ul.vehicle_id = :vehicleId "
            + "AND ul.measure_type = :measureType "
            + "AND ul.recorded_at < (CAST(:to AS date) + INTERVAL '1 day') AT TIME ZONE 'UTC' "
            + "ORDER BY ul.recorded_at DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findLatestValueUpToDate(@Param("vehicleId") UUID vehicleId, @Param("measureType") String measureType,
            @Param("to") LocalDate to);

    /**
     * Earliest ever reading for a vehicle under its CURRENT usage measure — used as the baseline
     * when {@code from} is unset (unbounded start) in
     * {@code ProfitabilityService.computeUsageInRange}. See {@link #findLatestValueBeforeDate} for
     * why {@code measureType} is a required filter.
     */
    @Query(value = "SELECT ul.value FROM usage_logs ul WHERE ul.vehicle_id = :vehicleId "
            + "AND ul.measure_type = :measureType "
            + "ORDER BY ul.recorded_at ASC LIMIT 1", nativeQuery = true)
    Optional<Long> findEarliestValue(@Param("vehicleId") UUID vehicleId, @Param("measureType") String measureType);
}

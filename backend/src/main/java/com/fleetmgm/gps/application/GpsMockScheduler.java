package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GpsMockScheduler {

    private static final Logger log = LoggerFactory.getLogger(GpsMockScheduler.class);

    // Major Spanish cities (same set already used as job origins in the V20 demo seed) — a vehicle
    // with no prior position starts near one chosen at random instead of always Madrid, so a fresh
    // demo fleet reads as genuinely national instead of a single cluster.
    public static final double[][] SPANISH_CITY_BASES = {
            {40.4168, -3.7038},  // Madrid
            {41.3851, 2.1734},   // Barcelona
            {39.4699, -0.3763},  // Valencia
            {37.3891, -5.9845},  // Sevilla
            {43.2630, -2.9350},  // Bilbao
            {41.6488, -0.8891},  // Zaragoza
            {36.7213, -4.4214},  // Málaga
            {37.9922, -1.1307},  // Murcia
            {38.3452, -0.4810},  // Alicante
            {41.6523, -4.7245},  // Valladolid
    };
    public static final double INITIAL_SPREAD_DEGREES = 0.05;
    public static final double DRIFT_DEGREES = 0.002;

    private final VehicleRepository vehicleRepository;
    private final GpsRepository gpsRepository;
    private final GpsMockState gpsMockState;
    private final AuditLogHelper auditLogHelper;

    public GpsMockScheduler(VehicleRepository vehicleRepository, GpsRepository gpsRepository,
                            GpsMockState gpsMockState, AuditLogHelper auditLogHelper) {
        this.vehicleRepository = vehicleRepository;
        this.gpsRepository = gpsRepository;
        this.gpsMockState = gpsMockState;
        this.auditLogHelper = auditLogHelper;
    }

    // The interval is configurable because this tick is the app's only permanent background cost:
    // every wake-up is a read plus N inserts, on a host that bills CPU and memory by the minute.
    // The guard below is what makes the default deployment cost nothing at all — see GpsMockState.
    @Scheduled(fixedDelayString = "${gps.mock.interval-ms:30000}")
    @Transactional
    public void generatePositions() {
        if (gpsMockState.disableIfLapsed()) {
            log.info("GPS mock generator auto-disabled: its activation window ran out");
            auditLogHelper.logSystem(GpsMockService.ENTITY_TYPE, GpsMockService.ENTITY_ID, AuditAction.UPDATE,
                    "GPS mock generator auto-disabled after its activation window ran out");
        }
        if (!gpsMockState.isActive()) {
            return;
        }

        List<Vehicle> activeVehicles = vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE);
        if (activeVehicles.isEmpty()) {
            return;
        }

        // One query for the whole fleet's last known position, not one per vehicle: this tick used to
        // cost 1 + 2N queries and degraded as the fleet grew. Vehicles with no history simply miss the
        // map and fall through to city-base seeding in nextPosition().
        Map<UUID, GpsPosition> lastKnownByVehicleId = gpsRepository.findLatestForAllActiveVehicles().stream()
                .collect(Collectors.toMap(
                        position -> position.getVehicle().getId(),
                        Function.identity(),
                        // Two rows can share the same MAX(recordedAt) for one vehicle; either is a valid
                        // drift anchor, so keep the first rather than letting toMap throw.
                        (first, duplicate) -> first));

        gpsRepository.saveAll(activeVehicles.stream()
                .map(vehicle -> nextPosition(vehicle, lastKnownByVehicleId.get(vehicle.getId())))
                .toList());
    }

    private GpsPosition nextPosition(Vehicle vehicle, GpsPosition previous) {
        GpsPosition position = new GpsPosition();
        position.setVehicle(vehicle);
        if (previous == null) {
            double[] cityBase = SPANISH_CITY_BASES[ThreadLocalRandom.current().nextInt(SPANISH_CITY_BASES.length)];
            position.setLatitude(randomAround(cityBase[0], INITIAL_SPREAD_DEGREES));
            position.setLongitude(randomAround(cityBase[1], INITIAL_SPREAD_DEGREES));
        } else {
            position.setLatitude(randomAround(previous.getLatitude(), DRIFT_DEGREES));
            position.setLongitude(randomAround(previous.getLongitude(), DRIFT_DEGREES));
        }
        position.setHeading(ThreadLocalRandom.current().nextDouble(0, 360));
        position.setSpeed(ThreadLocalRandom.current().nextDouble(0, 100));
        position.setRecordedAt(Instant.now());
        position.setSource(GpsSource.MOCK);
        return position;
    }

    private double randomAround(double center, double spread) {
        return center + ThreadLocalRandom.current().nextDouble(-spread, spread);
    }
}

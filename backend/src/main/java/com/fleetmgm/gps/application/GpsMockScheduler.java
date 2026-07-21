package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GpsMockScheduler {

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

    public GpsMockScheduler(VehicleRepository vehicleRepository, GpsRepository gpsRepository) {
        this.vehicleRepository = vehicleRepository;
        this.gpsRepository = gpsRepository;
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void generatePositions() {
        for (Vehicle vehicle : vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)) {
            GpsPosition previous = gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())
                    .orElse(null);
            gpsRepository.save(nextPosition(vehicle, previous));
        }
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

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

    // Madrid — arbitrary fleet home base; new vehicles with no prior position start here.
    public static final double BASE_LATITUDE = 40.4168;
    public static final double BASE_LONGITUDE = -3.7038;
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
        position.setLatitude(previous == null
                ? randomAround(BASE_LATITUDE, INITIAL_SPREAD_DEGREES)
                : randomAround(previous.getLatitude(), DRIFT_DEGREES));
        position.setLongitude(previous == null
                ? randomAround(BASE_LONGITUDE, INITIAL_SPREAD_DEGREES)
                : randomAround(previous.getLongitude(), DRIFT_DEGREES));
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

package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsMockSchedulerTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock GpsRepository gpsRepository;
    @InjectMocks GpsMockScheduler gpsMockScheduler;

    @Test
    void generatePositions_createsOnePositionPerActiveVehicle() {
        Vehicle vehicle1 = vehicleWithId(UUID.randomUUID());
        Vehicle vehicle2 = vehicleWithId(UUID.randomUUID());
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle1, vehicle2));
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        List<GpsPosition> saved = captureSavedPositions();
        assertThat(saved).extracting(GpsPosition::getVehicle).containsExactlyInAnyOrder(vehicle1, vehicle2);
        assertThat(saved).allSatisfy(position -> {
            assertThat(position.getSource()).isEqualTo(GpsSource.MOCK);
            assertThat(position.getRecordedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        });
    }

    // Regression guard for the N+1 this scheduler used to run: it looked up the previous position
    // one vehicle at a time, so a tick cost 1 + 2N queries and got worse as the fleet grew. The
    // whole fleet's last known position now comes back in a single query, whatever N is.
    @Test
    void generatePositions_readsPreviousPositions_inASingleQuery_regardlessOfFleetSize() {
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            vehicles.add(vehicleWithId(UUID.randomUUID()));
        }
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(vehicles);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        verify(gpsRepository).findLatestForAllActiveVehicles();
        verify(gpsRepository).saveAll(any());
        verify(gpsRepository, never()).save(any());
        assertThat(captureSavedPositions()).hasSize(40);
    }

    @Test
    void generatePositions_doesNothing_whenNoActiveVehicles() {
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        verify(gpsRepository, never()).saveAll(any());
        verify(gpsRepository, never()).findLatestForAllActiveVehicles();
    }

    @Test
    void generatePositions_coordinatesWithinInitialSpread_ofSomeCityBase_whenVehicleHasNoPriorPosition() {
        Vehicle vehicle = vehicleWithId(UUID.randomUUID());
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle));
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        GpsPosition saved = captureSavedPositions().getFirst();

        boolean withinSomeCityBase = Arrays.stream(GpsMockScheduler.SPANISH_CITY_BASES).anyMatch(city ->
                Math.abs(saved.getLatitude() - city[0]) <= GpsMockScheduler.INITIAL_SPREAD_DEGREES
                        && Math.abs(saved.getLongitude() - city[1]) <= GpsMockScheduler.INITIAL_SPREAD_DEGREES);
        assertThat(withinSomeCityBase).isTrue();
    }

    @Test
    void generatePositions_spreadsAcrossMultipleCities_whenManyVehiclesHaveNoPriorPosition() {
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            vehicles.add(vehicleWithId(UUID.randomUUID()));
        }
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(vehicles);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        Set<Integer> citiesUsed = captureSavedPositions().stream()
                .map(position -> closestCityIndex(position.getLatitude(), position.getLongitude()))
                .collect(Collectors.toSet());

        assertThat(citiesUsed.size()).isGreaterThan(1);
    }

    @Test
    void generatePositions_coordinatesWithinDriftRange_ofPreviousPosition() {
        Vehicle vehicle = vehicleWithId(UUID.randomUUID());
        GpsPosition previous = positionFor(vehicle, 41.0, -4.0);
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle));
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(previous));

        gpsMockScheduler.generatePositions();

        GpsPosition saved = captureSavedPositions().getFirst();
        assertThat(saved.getLatitude()).isBetween(
                previous.getLatitude() - GpsMockScheduler.DRIFT_DEGREES,
                previous.getLatitude() + GpsMockScheduler.DRIFT_DEGREES);
        assertThat(saved.getLongitude()).isBetween(
                previous.getLongitude() - GpsMockScheduler.DRIFT_DEGREES,
                previous.getLongitude() + GpsMockScheduler.DRIFT_DEGREES);
    }

    // A vehicle that already has a position and one that does not are served by the same batch
    // lookup, so the map miss for the newcomer must still fall back to the city-base seeding.
    @Test
    void generatePositions_seedsFromCityBase_forVehiclesMissingFromTheBatchLookup() {
        Vehicle known = vehicleWithId(UUID.randomUUID());
        Vehicle newcomer = vehicleWithId(UUID.randomUUID());
        GpsPosition previous = positionFor(known, 41.0, -4.0);
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(known, newcomer));
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(previous));

        gpsMockScheduler.generatePositions();

        List<GpsPosition> saved = captureSavedPositions();
        assertThat(saved).hasSize(2);

        GpsPosition drifted = saved.stream().filter(p -> p.getVehicle() == known).findFirst().orElseThrow();
        assertThat(drifted.getLatitude()).isBetween(
                41.0 - GpsMockScheduler.DRIFT_DEGREES, 41.0 + GpsMockScheduler.DRIFT_DEGREES);

        GpsPosition seeded = saved.stream().filter(p -> p.getVehicle() == newcomer).findFirst().orElseThrow();
        boolean withinSomeCityBase = Arrays.stream(GpsMockScheduler.SPANISH_CITY_BASES).anyMatch(city ->
                Math.abs(seeded.getLatitude() - city[0]) <= GpsMockScheduler.INITIAL_SPREAD_DEGREES
                        && Math.abs(seeded.getLongitude() - city[1]) <= GpsMockScheduler.INITIAL_SPREAD_DEGREES);
        assertThat(withinSomeCityBase).isTrue();
    }

    @SuppressWarnings("unchecked")
    private List<GpsPosition> captureSavedPositions() {
        ArgumentCaptor<List<GpsPosition>> captor = ArgumentCaptor.forClass(List.class);
        verify(gpsRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private static int closestCityIndex(double latitude, double longitude) {
        int closest = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < GpsMockScheduler.SPANISH_CITY_BASES.length; i++) {
            double[] city = GpsMockScheduler.SPANISH_CITY_BASES[i];
            double distance = Math.hypot(latitude - city[0], longitude - city[1]);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = i;
            }
        }
        return closest;
    }

    private static GpsPosition positionFor(Vehicle vehicle, double latitude, double longitude) {
        GpsPosition position = new GpsPosition();
        position.setVehicle(vehicle);
        position.setLatitude(latitude);
        position.setLongitude(longitude);
        return position;
    }

    private static Vehicle vehicleWithId(UUID id) {
        Vehicle vehicle = new Vehicle();
        setId(vehicle, id);
        return vehicle;
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

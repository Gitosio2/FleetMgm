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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(any())).thenReturn(Optional.empty());

        gpsMockScheduler.generatePositions();

        ArgumentCaptor<GpsPosition> captor = ArgumentCaptor.forClass(GpsPosition.class);
        verify(gpsRepository, times(2)).save(captor.capture());
        List<GpsPosition> saved = captor.getAllValues();
        assertThat(saved).extracting(GpsPosition::getVehicle).containsExactlyInAnyOrder(vehicle1, vehicle2);
        assertThat(saved).allSatisfy(position -> {
            assertThat(position.getSource()).isEqualTo(GpsSource.MOCK);
            assertThat(position.getRecordedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        });
    }

    @Test
    void generatePositions_doesNothing_whenNoActiveVehicles() {
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of());

        gpsMockScheduler.generatePositions();

        verify(gpsRepository, never()).save(any());
    }

    @Test
    void generatePositions_coordinatesWithinInitialSpread_ofSomeCityBase_whenVehicleHasNoPriorPosition() {
        Vehicle vehicle = vehicleWithId(UUID.randomUUID());
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle));
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())).thenReturn(Optional.empty());

        gpsMockScheduler.generatePositions();

        ArgumentCaptor<GpsPosition> captor = ArgumentCaptor.forClass(GpsPosition.class);
        verify(gpsRepository).save(captor.capture());
        GpsPosition saved = captor.getValue();

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
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(any())).thenReturn(Optional.empty());

        gpsMockScheduler.generatePositions();

        ArgumentCaptor<GpsPosition> captor = ArgumentCaptor.forClass(GpsPosition.class);
        verify(gpsRepository, times(50)).save(captor.capture());

        Set<Integer> citiesUsed = captor.getAllValues().stream()
                .map(position -> closestCityIndex(position.getLatitude(), position.getLongitude()))
                .collect(Collectors.toSet());

        assertThat(citiesUsed.size()).isGreaterThan(1);
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

    @Test
    void generatePositions_coordinatesWithinDriftRange_ofPreviousPosition() {
        Vehicle vehicle = vehicleWithId(UUID.randomUUID());
        GpsPosition previous = new GpsPosition();
        previous.setLatitude(41.0);
        previous.setLongitude(-4.0);
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle));
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())).thenReturn(Optional.of(previous));

        gpsMockScheduler.generatePositions();

        ArgumentCaptor<GpsPosition> captor = ArgumentCaptor.forClass(GpsPosition.class);
        verify(gpsRepository).save(captor.capture());
        GpsPosition saved = captor.getValue();
        assertThat(saved.getLatitude()).isBetween(
                previous.getLatitude() - GpsMockScheduler.DRIFT_DEGREES,
                previous.getLatitude() + GpsMockScheduler.DRIFT_DEGREES);
        assertThat(saved.getLongitude()).isBetween(
                previous.getLongitude() - GpsMockScheduler.DRIFT_DEGREES,
                previous.getLongitude() + GpsMockScheduler.DRIFT_DEGREES);
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

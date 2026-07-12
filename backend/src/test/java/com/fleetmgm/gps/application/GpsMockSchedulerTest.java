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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    void generatePositions_coordinatesWithinInitialSpread_whenVehicleHasNoPriorPosition() {
        Vehicle vehicle = vehicleWithId(UUID.randomUUID());
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE)).thenReturn(List.of(vehicle));
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())).thenReturn(Optional.empty());

        gpsMockScheduler.generatePositions();

        ArgumentCaptor<GpsPosition> captor = ArgumentCaptor.forClass(GpsPosition.class);
        verify(gpsRepository).save(captor.capture());
        GpsPosition saved = captor.getValue();
        assertThat(saved.getLatitude()).isBetween(
                GpsMockScheduler.BASE_LATITUDE - GpsMockScheduler.INITIAL_SPREAD_DEGREES,
                GpsMockScheduler.BASE_LATITUDE + GpsMockScheduler.INITIAL_SPREAD_DEGREES);
        assertThat(saved.getLongitude()).isBetween(
                GpsMockScheduler.BASE_LONGITUDE - GpsMockScheduler.INITIAL_SPREAD_DEGREES,
                GpsMockScheduler.BASE_LONGITUDE + GpsMockScheduler.INITIAL_SPREAD_DEGREES);
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

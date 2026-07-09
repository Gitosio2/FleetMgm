package com.fleetmgm.vehicle.application;

import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.VehicleEntersWorkshopEvent;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceEventListenerTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @InjectMocks MaintenanceEventListener maintenanceEventListener;

    @Test
    void onVehicleEntersWorkshop_setsVehicleToMaintenance() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.ACTIVE);
        VehicleEntersWorkshopEvent event = new VehicleEntersWorkshopEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        maintenanceEventListener.onVehicleEntersWorkshop(event);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void onMaintenanceCompleted_setsVehicleToActive() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        maintenanceEventListener.onMaintenanceCompleted(event);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void onMaintenanceCompleted_keepsVehicleInWorkshop_whenAnotherMaintenanceInProgress() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(maintenanceRepository.existsByVehicleIdAndStatus(vehicleId, MaintenanceStatus.IN_PROGRESS))
                .thenReturn(true);

        maintenanceEventListener.onMaintenanceCompleted(event);

        // vehicle still has open workshop work — its status must not be touched
        verifyNoInteractions(vehicleRepository);
    }
}

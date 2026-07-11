package com.fleetmgm.vehicle.application;

import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceCancelledEvent;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void onMaintenanceCancelled_setsVehicleToActive() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        maintenanceEventListener.onMaintenanceCancelled(event);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void onMaintenanceCancelled_keepsVehicleInWorkshop_whenAnotherMaintenanceInProgress() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(maintenanceRepository.existsByVehicleIdAndStatus(vehicleId, MaintenanceStatus.IN_PROGRESS))
                .thenReturn(true);

        maintenanceEventListener.onMaintenanceCancelled(event);

        // another maintenance still holds the vehicle in the workshop — its status must not be touched
        verifyNoInteractions(vehicleRepository);
    }

    // --- Bug 1 (BLOCKER): a vehicle that left MAINTENANCE through an unrelated path (or never
    // entered it, e.g. cancelling a stale SCHEDULED record) must never be force-reactivated just
    // because no other maintenance is IN_PROGRESS. Only a vehicle whose CURRENT status is actually
    // MAINTENANCE may be flipped back to ACTIVE. ---

    @Test
    void onMaintenanceCancelled_doesNotReactivateVehicle_whenVehicleNotCurrentlyInMaintenance() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.DECOMMISSIONED);
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        maintenanceEventListener.onMaintenanceCancelled(event);

        // vehicle was decommissioned via an unrelated path — cancelling a stale maintenance record
        // that never touched it must not un-decommission it
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.DECOMMISSIONED);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void onMaintenanceCompleted_doesNotReactivateVehicle_whenVehicleNotCurrentlyInMaintenance() {
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus(VehicleStatus.INACTIVE);
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        maintenanceEventListener.onMaintenanceCompleted(event);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.INACTIVE);
        verify(vehicleRepository, never()).save(any());
    }

    // --- Bug/gap 2 (CRITICAL): AFTER_COMMIT listeners must never let an exception propagate —
    // the triggering transaction already committed and the original HTTP call already returned
    // 200 OK, so a cascade failure must be logged, not silently lost or rethrown. ---

    @Test
    void onVehicleEntersWorkshop_doesNotPropagate_whenRepositoryThrows() {
        UUID vehicleId = UUID.randomUUID();
        VehicleEntersWorkshopEvent event = new VehicleEntersWorkshopEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(vehicleRepository.findById(vehicleId)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> maintenanceEventListener.onVehicleEntersWorkshop(event))
                .doesNotThrowAnyException();
    }

    @Test
    void onMaintenanceCompleted_doesNotPropagate_whenRepositoryThrows() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceCompletedEvent event = new MaintenanceCompletedEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(maintenanceRepository.existsByVehicleIdAndStatus(vehicleId, MaintenanceStatus.IN_PROGRESS))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> maintenanceEventListener.onMaintenanceCompleted(event))
                .doesNotThrowAnyException();
    }

    @Test
    void onMaintenanceCancelled_doesNotPropagate_whenRepositoryThrows() {
        UUID vehicleId = UUID.randomUUID();
        MaintenanceCancelledEvent event = new MaintenanceCancelledEvent(
                UUID.randomUUID(), vehicleId, Instant.now());

        when(maintenanceRepository.existsByVehicleIdAndStatus(vehicleId, MaintenanceStatus.IN_PROGRESS))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> maintenanceEventListener.onMaintenanceCancelled(event))
                .doesNotThrowAnyException();
    }
}

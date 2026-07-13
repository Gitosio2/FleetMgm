package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceCancelledEvent;
import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.VehicleEntersWorkshopEvent;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class MaintenanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceEventListener.class);

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;

    public MaintenanceEventListener(VehicleRepository vehicleRepository,
                                    MaintenanceRepository maintenanceRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVehicleEntersWorkshop(VehicleEntersWorkshopEvent event) {
        // AFTER_COMMIT: the triggering transaction already committed and the original HTTP call
        // already returned 200 OK, so an exception here can't roll anything back — it must be
        // logged with enough context to debug later, never left silent and never rethrown.
        try {
            updateStatus(event.vehicleId(), VehicleStatus.MAINTENANCE);
        } catch (RuntimeException e) {
            log.error("Failed to set vehicle {} to MAINTENANCE for maintenance record {}",
                    event.vehicleId(), event.maintenanceId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMaintenanceCompleted(MaintenanceCompletedEvent event) {
        try {
            reactivateVehicleIfNoOtherActiveMaintenance(event.vehicleId());
        } catch (RuntimeException e) {
            log.error("Failed to reactivate vehicle {} after maintenance record {} completed",
                    event.vehicleId(), event.maintenanceId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMaintenanceCancelled(MaintenanceCancelledEvent event) {
        // MaintenanceService.cancel() publishes this event regardless of the cancelled record's prior
        // state (SCHEDULED or IN_PROGRESS) so ScheduleCancellationListener can always cascade to the
        // linked WorkshopSchedule. On the vehicle side, reactivateVehicleIfNoOtherActiveMaintenance's
        // own current-status guard is what keeps this a no-op when the record never touched the
        // vehicle in the first place (cancelled while still SCHEDULED).
        try {
            reactivateVehicleIfNoOtherActiveMaintenance(event.vehicleId());
        } catch (RuntimeException e) {
            log.error("Failed to reactivate vehicle {} after maintenance record {} cancelled",
                    event.vehicleId(), event.maintenanceId(), e);
        }
    }

    // The vehicle only leaves the workshop once no other maintenance is still IN_PROGRESS for it
    // (SCHEDULED maintenances don't count — a vehicle enters MAINTENANCE on start(), not on create()),
    // AND its CURRENT status is actually MAINTENANCE. That second guard is load-bearing: without it, a
    // vehicle that left MAINTENANCE through an unrelated path (or never entered it — e.g. cancelling a
    // stale SCHEDULED record) would get silently force-reactivated to ACTIVE, e.g. un-decommissioning
    // a DECOMMISSIONED vehicle.
    private void reactivateVehicleIfNoOtherActiveMaintenance(UUID vehicleId) {
        if (maintenanceRepository.existsByVehicleIdAndStatus(vehicleId, MaintenanceStatus.IN_PROGRESS)) {
            return;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + vehicleId + " not found"));
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            return;
        }
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicleRepository.save(vehicle);
    }

    private void updateStatus(UUID vehicleId, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + vehicleId + " not found"));
        vehicle.setStatus(status);
        vehicleRepository.save(vehicle);
    }
}

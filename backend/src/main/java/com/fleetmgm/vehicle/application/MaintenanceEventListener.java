package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.MaintenanceCompletedEvent;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.domain.VehicleEntersWorkshopEvent;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class MaintenanceEventListener {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;

    public MaintenanceEventListener(VehicleRepository vehicleRepository,
                                    MaintenanceRepository maintenanceRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onVehicleEntersWorkshop(VehicleEntersWorkshopEvent event) {
        updateStatus(event.vehicleId(), VehicleStatus.MAINTENANCE);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onMaintenanceCompleted(MaintenanceCompletedEvent event) {
        // The vehicle only leaves the workshop once no other maintenance is still IN_PROGRESS.
        // SCHEDULED maintenances don't count — a vehicle enters MAINTENANCE on start(), not on create().
        if (maintenanceRepository.existsByVehicleIdAndStatus(event.vehicleId(), MaintenanceStatus.IN_PROGRESS)) {
            return;
        }
        updateStatus(event.vehicleId(), VehicleStatus.ACTIVE);
    }

    private void updateStatus(UUID vehicleId, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + vehicleId + " not found"));
        vehicle.setStatus(status);
        vehicleRepository.save(vehicle);
    }
}

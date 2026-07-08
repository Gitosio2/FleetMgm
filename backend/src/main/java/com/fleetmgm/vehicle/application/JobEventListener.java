package com.fleetmgm.vehicle.application;

import com.fleetmgm.job.domain.JobCompletedEvent;
import com.fleetmgm.job.infrastructure.JobRepository;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.UsageLog;
import com.fleetmgm.vehicle.domain.UsageSource;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.UsageLogRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JobEventListener {

    private final VehicleRepository vehicleRepository;
    private final UsageLogRepository usageLogRepository;
    private final JobRepository jobRepository;

    public JobEventListener(VehicleRepository vehicleRepository,
                            UsageLogRepository usageLogRepository,
                            JobRepository jobRepository) {
        this.vehicleRepository = vehicleRepository;
        this.usageLogRepository = usageLogRepository;
        this.jobRepository = jobRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onJobCompleted(JobCompletedEvent event) {
        // No usage value recorded at completion time -> nothing to update.
        if (event.endUsageValue() == null) {
            return;
        }

        Vehicle vehicle = vehicleRepository.findById(event.vehicleId())
                .orElseThrow(() -> new NotFoundException("VEHICLE_NOT_FOUND",
                        "Vehicle " + event.vehicleId() + " not found"));

        switch (vehicle.getUsageMeasure()) {
            case KILOMETERS -> vehicle.setCurrentKm(event.endUsageValue());
            case HOURS -> vehicle.setCurrentHours(event.endUsageValue());
            default -> throw new IllegalStateException("Unhandled usage measure: " + vehicle.getUsageMeasure());
        }
        vehicleRepository.save(vehicle);

        UsageLog usageLog = new UsageLog();
        usageLog.setVehicle(vehicle);
        usageLog.setValue(event.endUsageValue());
        usageLog.setMeasureType(vehicle.getUsageMeasure());
        usageLog.setRecordedAt(event.completedAt());
        usageLog.setSource(UsageSource.JOB_COMPLETION);
        usageLog.setJob(jobRepository.getReferenceById(event.jobId()));
        usageLogRepository.save(usageLog);
    }
}

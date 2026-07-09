package com.fleetmgm.workshop.domain;

import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.worker.domain.Worker;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workshop_schedules")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class WorkshopSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Worker technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_record_id")
    private MaintenanceRecord maintenanceRecord;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false, length = 100)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SchedulePriority priority = SchedulePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private WorkshopStatus status = WorkshopStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() { return id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Worker getTechnician() { return technician; }
    public void setTechnician(Worker technician) { this.technician = technician; }

    public MaintenanceRecord getMaintenanceRecord() { return maintenanceRecord; }
    public void setMaintenanceRecord(MaintenanceRecord maintenanceRecord) { this.maintenanceRecord = maintenanceRecord; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public SchedulePriority getPriority() { return priority; }
    public void setPriority(SchedulePriority priority) { this.priority = priority; }

    public WorkshopStatus getStatus() { return status; }
    public void setStatus(WorkshopStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}

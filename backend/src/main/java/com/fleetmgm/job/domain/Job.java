package com.fleetmgm.job.domain;

import com.fleetmgm.client.domain.Client;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.worker.domain.Worker;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Worker assignedDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "origin_location", nullable = false, length = 500)
    private String originLocation;

    @Column(name = "destination_location", nullable = false, length = 500)
    private String destinationLocation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "start_usage_value")
    private Long startUsageValue;

    @Column(name = "end_usage_value")
    private Long endUsageValue;

    // Nullable: a Job with no clientId bills nothing (no-op in the JobCompletedEvent consumer). A
    // Job with a clientId but no price still gets billed — the consumer adds a 0.00 line item to
    // invoice instead of silently dropping the billable work (see InvoiceJobCompletionListener).
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Optimistic lock: rejects a concurrent status transition (start/complete/cancel) against a
    // stale copy instead of silently letting the second writer win and double-fire JobCompletedEvent.
    @Version
    @Column(nullable = false)
    private Long version;

    public UUID getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Worker getAssignedDriver() { return assignedDriver; }
    public void setAssignedDriver(Worker assignedDriver) { this.assignedDriver = assignedDriver; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getOriginLocation() { return originLocation; }
    public void setOriginLocation(String originLocation) { this.originLocation = originLocation; }

    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(Instant scheduledStart) { this.scheduledStart = scheduledStart; }

    public Instant getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(Instant scheduledEnd) { this.scheduledEnd = scheduledEnd; }

    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }

    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }

    public Long getStartUsageValue() { return startUsageValue; }
    public void setStartUsageValue(Long startUsageValue) { this.startUsageValue = startUsageValue; }

    public Long getEndUsageValue() { return endUsageValue; }
    public void setEndUsageValue(Long endUsageValue) { this.endUsageValue = endUsageValue; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public Long getVersion() { return version; }
}

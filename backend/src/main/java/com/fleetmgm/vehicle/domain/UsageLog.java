package com.fleetmgm.vehicle.domain;

import com.fleetmgm.job.domain.Job;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_logs")
@EntityListeners(AuditingEntityListener.class)
public class UsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private Long value;

    @Enumerated(EnumType.STRING)
    @Column(name = "measure_type", nullable = false, length = 10)
    private UsageMeasure measureType;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsageSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }

    public UsageMeasure getMeasureType() { return measureType; }
    public void setMeasureType(UsageMeasure measureType) { this.measureType = measureType; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public UsageSource getSource() { return source; }
    public void setSource(UsageSource source) { this.source = source; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Instant getCreatedAt() { return createdAt; }
}

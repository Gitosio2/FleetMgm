package com.fleetmgm.workshop.domain;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.worker.domain.Worker;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_records")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_at_service")
    private Long usageAtService;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "workshop_entry_date")
    private LocalDate workshopEntryDate;

    @Column(name = "workshop_exit_date")
    private LocalDate workshopExitDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Worker technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MaintenanceCategory category = MaintenanceCategory.PREVENTIVE;

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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUsageAtService() { return usageAtService; }
    public void setUsageAtService(Long usageAtService) { this.usageAtService = usageAtService; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public LocalDate getWorkshopEntryDate() { return workshopEntryDate; }
    public void setWorkshopEntryDate(LocalDate workshopEntryDate) { this.workshopEntryDate = workshopEntryDate; }

    public LocalDate getWorkshopExitDate() { return workshopExitDate; }
    public void setWorkshopExitDate(LocalDate workshopExitDate) { this.workshopExitDate = workshopExitDate; }

    public Worker getTechnician() { return technician; }
    public void setTechnician(Worker technician) { this.technician = technician; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { this.status = status; }

    public MaintenanceCategory getCategory() { return category; }
    public void setCategory(MaintenanceCategory category) { this.category = category; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}

package com.fleetmgm.vehicle.domain;

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
@Table(name = "vehicles")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, length = 20)
    private VehicleCategory vehicleCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_measure", nullable = false, length = 10)
    private UsageMeasure usageMeasure;

    @Column(name = "heavy_subtype", length = 100)
    private String heavySubtype;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(length = 17)
    private String vin;

    @Column(length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "current_km")
    private Long currentKm;

    @Column(name = "current_hours")
    private Long currentHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_type", length = 10)
    private AcquisitionType acquisitionType;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "amortization_years")
    private Integer amortizationYears;

    /**
     * For LEASING/RENTING: the monthly contract fee.
     * For PURCHASED: optional manual override of the computed amortization value.
     */
    @Column(name = "monthly_fee", precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId() { return id; }

    public VehicleCategory getVehicleCategory() { return vehicleCategory; }
    public void setVehicleCategory(VehicleCategory vehicleCategory) { this.vehicleCategory = vehicleCategory; }

    public UsageMeasure getUsageMeasure() { return usageMeasure; }
    public void setUsageMeasure(UsageMeasure usageMeasure) { this.usageMeasure = usageMeasure; }

    public String getHeavySubtype() { return heavySubtype; }
    public void setHeavySubtype(String heavySubtype) { this.heavySubtype = heavySubtype; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    public Long getCurrentKm() { return currentKm; }
    public void setCurrentKm(Long currentKm) { this.currentKm = currentKm; }

    public Long getCurrentHours() { return currentHours; }
    public void setCurrentHours(Long currentHours) { this.currentHours = currentHours; }

    // Reads/writes currentKm or currentHours depending on usageMeasure, so callers don't duplicate the switch.
    public Long getCurrentUsageValue() {
        return switch (usageMeasure) {
            case KILOMETERS -> currentKm;
            case HOURS -> currentHours;
            default -> throw new IllegalStateException("Unhandled usage measure: " + usageMeasure);
        };
    }

    public void setCurrentUsageValue(Long value) {
        switch (usageMeasure) {
            case KILOMETERS -> currentKm = value;
            case HOURS -> currentHours = value;
            default -> throw new IllegalStateException("Unhandled usage measure: " + usageMeasure);
        }
    }

    public AcquisitionType getAcquisitionType() { return acquisitionType; }
    public void setAcquisitionType(AcquisitionType acquisitionType) { this.acquisitionType = acquisitionType; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public Integer getAmortizationYears() { return amortizationYears; }
    public void setAmortizationYears(Integer amortizationYears) { this.amortizationYears = amortizationYears; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}

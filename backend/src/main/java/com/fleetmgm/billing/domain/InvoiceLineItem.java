package com.fleetmgm.billing.domain;

import com.fleetmgm.job.domain.Job;
import com.fleetmgm.workshop.domain.MaintenanceRecord;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_job_id")
    private Job linkedJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_maintenance_id")
    private MaintenanceRecord linkedMaintenance;

    public UUID getId() { return id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Job getLinkedJob() { return linkedJob; }
    public void setLinkedJob(Job linkedJob) { this.linkedJob = linkedJob; }

    public MaintenanceRecord getLinkedMaintenance() { return linkedMaintenance; }
    public void setLinkedMaintenance(MaintenanceRecord linkedMaintenance) { this.linkedMaintenance = linkedMaintenance; }
}

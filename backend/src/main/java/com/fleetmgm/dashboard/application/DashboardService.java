package com.fleetmgm.dashboard.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceRepository;
import com.fleetmgm.dashboard.dto.FinancialSummaryResponse;
import com.fleetmgm.dashboard.dto.FleetSummaryResponse;
import com.fleetmgm.dashboard.dto.UpcomingInvoiceResponse;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class DashboardService {

    // Matches the Matriz de Permisos row for fleet-wide KPI/report visibility — mirrors
    // ProfitabilityService.ROLES/InvoiceService.ROLES/SupplierInvoiceService.ROLES verbatim.
    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    // "Due within 48 hours" is measured in whole LocalDate days (scheduledDate has no time
    // component), so the practical boundary is today through today + 2 days inclusive.
    private static final int DUE_SOON_WINDOW_DAYS = 2;

    // "Due soon" window for the financial summary's upcoming receivables/payables — a wider,
    // week-long horizon than the workshop's 48h window since invoices are planned further ahead.
    private static final int DUE_SOON_INVOICE_WINDOW_DAYS = 7;

    private static final int UPCOMING_INVOICES_LIMIT = 5;

    private final VehicleRepository vehicleRepository;
    private final WorkshopScheduleRepository workshopScheduleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardService(VehicleRepository vehicleRepository,
                             WorkshopScheduleRepository workshopScheduleRepository,
                             MaintenanceRepository maintenanceRepository,
                             SupplierInvoiceRepository supplierInvoiceRepository,
                             InvoiceRepository invoiceRepository) {
        this.vehicleRepository = vehicleRepository;
        this.workshopScheduleRepository = workshopScheduleRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public FleetSummaryResponse getFleetSummary() {
        long activeVehicles = vehicleRepository.countByStatus(VehicleStatus.ACTIVE);
        long totalVehicles = vehicleRepository.countByStatusNot(VehicleStatus.DECOMMISSIONED);
        long inWorkshop = vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE);

        long pendingMaintenance = workshopScheduleRepository.countByStatus(WorkshopStatus.PENDING);
        LocalDate today = LocalDate.now();
        long pendingMaintenanceDueSoon = workshopScheduleRepository.countByStatusAndScheduledDateBetween(
                WorkshopStatus.PENDING, today, today.plusDays(DUE_SOON_WINDOW_DAYS));

        return new FleetSummaryResponse(
                activeVehicles, totalVehicles, inWorkshop, pendingMaintenance, pendingMaintenanceDueSoon);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public FinancialSummaryResponse getFinancialSummary() {
        LocalDate today = LocalDate.now();
        LocalDate dueSoonCutoff = today.plusDays(DUE_SOON_INVOICE_WINDOW_DAYS);

        List<Invoice> receivables = invoiceRepository.findUpcomingReceivables(
                dueSoonCutoff, PageRequest.of(0, UPCOMING_INVOICES_LIMIT));
        List<SupplierInvoice> payables = supplierInvoiceRepository.findUpcomingPayables(
                dueSoonCutoff, PageRequest.of(0, UPCOMING_INVOICES_LIMIT));

        List<UpcomingInvoiceResponse> upcomingReceivables = receivables.stream()
                .map(invoice -> new UpcomingInvoiceResponse(
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        invoice.getClient().getName(),
                        invoice.getTotal(),
                        invoice.getDueDate(),
                        invoice.getDueDate().isBefore(today)))
                .toList();

        List<UpcomingInvoiceResponse> upcomingPayables = payables.stream()
                .map(supplierInvoice -> new UpcomingInvoiceResponse(
                        supplierInvoice.getId(),
                        supplierInvoice.getSupplierInvoiceNumber(),
                        supplierInvoice.getSupplier().getName(),
                        supplierInvoice.getTotal(),
                        supplierInvoice.getDueDate(),
                        supplierInvoice.getDueDate().isBefore(today)))
                .toList();

        return new FinancialSummaryResponse(monthlyCosts(), upcomingReceivables, upcomingPayables);
    }

    private BigDecimal monthlyCosts() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        BigDecimal supplierCosts = supplierInvoiceRepository.sumTotalByInvoiceDateBetween(monthStart, monthEnd);
        BigDecimal maintenanceCosts = maintenanceRepository.sumCostByWorkshopEntryDateBetween(monthStart, monthEnd);
        return supplierCosts.add(maintenanceCosts);
    }
}

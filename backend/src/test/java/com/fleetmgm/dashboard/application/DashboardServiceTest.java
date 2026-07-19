package com.fleetmgm.dashboard.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.MonthlyFinancialProjection;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.dashboard.dto.FinancialSummaryResponse;
import com.fleetmgm.dashboard.dto.FleetSummaryResponse;
import com.fleetmgm.dashboard.dto.UpcomingInvoiceResponse;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.infrastructure.MaintenanceRepository;
import com.fleetmgm.workshop.infrastructure.WorkshopScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock WorkshopScheduleRepository workshopScheduleRepository;
    @Mock MaintenanceRepository maintenanceRepository;
    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock ProfitabilityRepository profitabilityRepository;

    private DashboardService dashboardService() {
        return new DashboardService(
                vehicleRepository, workshopScheduleRepository, maintenanceRepository,
                supplierInvoiceRepository, invoiceRepository, profitabilityRepository);
    }

    private MonthlyFinancialProjection monthlyProjection(String month, BigDecimal revenue, BigDecimal costs) {
        return new MonthlyFinancialProjection() {
            @Override
            public String getMonth() {
                return month;
            }

            @Override
            public BigDecimal getRevenue() {
                return revenue;
            }

            @Override
            public BigDecimal getCosts() {
                return costs;
            }
        };
    }

    @Test
    void getFleetSummary_assemblesAllFields_fromRepositoryCalls() {
        when(vehicleRepository.countByStatus(VehicleStatus.ACTIVE)).thenReturn(12L);
        when(vehicleRepository.countByStatusNot(VehicleStatus.DECOMMISSIONED)).thenReturn(15L);
        when(vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE)).thenReturn(2L);
        when(workshopScheduleRepository.countByStatus(WorkshopStatus.PENDING)).thenReturn(4L);
        when(workshopScheduleRepository.countByStatusAndScheduledDateBetween(
                eq(WorkshopStatus.PENDING), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);

        FleetSummaryResponse summary = dashboardService().getFleetSummary();

        assertThat(summary.activeVehicles()).isEqualTo(12);
        assertThat(summary.totalVehicles()).isEqualTo(15);
        assertThat(summary.inWorkshop()).isEqualTo(2);
        assertThat(summary.pendingMaintenance()).isEqualTo(4);
        assertThat(summary.pendingMaintenanceDueSoon()).isEqualTo(1);
    }

    @Test
    void getFleetSummary_passesTodayThroughPlusTwoDays_forDueSoonWindow() {
        when(vehicleRepository.countByStatus(any())).thenReturn(0L);
        when(vehicleRepository.countByStatusNot(any())).thenReturn(0L);
        when(workshopScheduleRepository.countByStatus(any())).thenReturn(0L);
        when(workshopScheduleRepository.countByStatusAndScheduledDateBetween(any(), any(), any())).thenReturn(0L);

        dashboardService().getFleetSummary();

        LocalDate today = LocalDate.now();
        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(workshopScheduleRepository).countByStatusAndScheduledDateBetween(
                eq(WorkshopStatus.PENDING), from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(today);
        assertThat(to.getValue()).isEqualTo(today.plusDays(2));
    }

    @Test
    void getFinancialSummary_computesMonthlyCosts_fromSupplierAndMaintenanceSums() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any()))
                .thenReturn(new BigDecimal("6000.00"));
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any()))
                .thenReturn(new BigDecimal("2420.50"));
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.monthlyCosts()).isEqualByComparingTo("8420.50");
    }

    @Test
    void getFinancialSummary_passesCurrentMonthBounds_toCostSumQueries() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        dashboardService().getFinancialSummary();

        YearMonth currentMonth = YearMonth.now();
        LocalDate expectedStart = currentMonth.atDay(1);
        LocalDate expectedEnd = currentMonth.atEndOfMonth();

        ArgumentCaptor<LocalDate> supplierFrom = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> supplierTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(supplierInvoiceRepository).sumTotalByInvoiceDateBetween(supplierFrom.capture(), supplierTo.capture());
        assertThat(supplierFrom.getValue()).isEqualTo(expectedStart);
        assertThat(supplierTo.getValue()).isEqualTo(expectedEnd);

        ArgumentCaptor<LocalDate> maintenanceFrom = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> maintenanceTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(maintenanceRepository).sumCostByWorkshopEntryDateBetween(
                maintenanceFrom.capture(), maintenanceTo.capture());
        assertThat(maintenanceFrom.getValue()).isEqualTo(expectedStart);
        assertThat(maintenanceTo.getValue()).isEqualTo(expectedEnd);
    }

    @Test
    void getFinancialSummary_passesTodayThroughPlusSevenDays_forDueSoonWindow() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        dashboardService().getFinancialSummary();

        LocalDate today = LocalDate.now();

        ArgumentCaptor<LocalDate> receivablesTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(invoiceRepository).findUpcomingReceivables(receivablesTo.capture(), any());
        assertThat(receivablesTo.getValue()).isEqualTo(today.plusDays(7));

        ArgumentCaptor<LocalDate> payablesTo = ArgumentCaptor.forClass(LocalDate.class);
        verify(supplierInvoiceRepository).findUpcomingPayables(payablesTo.capture(), any());
        assertThat(payablesTo.getValue()).isEqualTo(today.plusDays(7));
    }

    @Test
    void getFinancialSummary_mapsUpcomingReceivable_notOverdue_whenDueDateIsInTheFuture() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        Client client = client("Acme Logistics");
        Invoice invoice = invoice(client, "INV-2026-00010", LocalDate.now().plusDays(3), new BigDecimal("500.00"));
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of(invoice));

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.upcomingReceivables()).hasSize(1);
        UpcomingInvoiceResponse response = summary.upcomingReceivables().get(0);
        assertThat(response.number()).isEqualTo("INV-2026-00010");
        assertThat(response.counterpartyId()).isEqualTo(client.getId());
        assertThat(response.counterparty()).isEqualTo("Acme Logistics");
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.overdue()).isFalse();
    }

    @Test
    void getFinancialSummary_mapsUpcomingReceivable_asOverdue_whenDueDateIsInThePast() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        Client client = client("Transportes Ibérica");
        Invoice invoice = invoice(client, "INV-2026-00011", LocalDate.now().minusDays(2), new BigDecimal("750.00"));
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of(invoice));

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.upcomingReceivables()).hasSize(1);
        assertThat(summary.upcomingReceivables().get(0).overdue()).isTrue();
    }

    @Test
    void getFinancialSummary_mapsUpcomingPayable_fromSupplierInvoice() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        Supplier supplier = supplier("Taller Mecánico Norte");
        SupplierInvoice supplierInvoice = supplierInvoice(
                supplier, "F-2026-0456", LocalDate.now().minusDays(1), new BigDecimal("121.00"));
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of(supplierInvoice));

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.upcomingPayables()).hasSize(1);
        UpcomingInvoiceResponse response = summary.upcomingPayables().get(0);
        assertThat(response.number()).isEqualTo("F-2026-0456");
        assertThat(response.counterpartyId()).isEqualTo(supplier.getId());
        assertThat(response.counterparty()).isEqualTo("Taller Mecánico Norte");
        assertThat(response.amount()).isEqualByComparingTo("121.00");
        assertThat(response.overdue()).isTrue();
    }

    @Test
    void getFinancialSummary_computesMonthlyRevenue_fromCurrentMonthTrendRow() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());

        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of(
                monthlyProjection(previousMonth.toString(), new BigDecimal("5000.00"), new BigDecimal("3000.00")),
                monthlyProjection(currentMonth.toString(), new BigDecimal("9500.00"), new BigDecimal("6000.00"))));

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.monthlyRevenue()).isEqualByComparingTo("9500.00");
    }

    @Test
    void getFinancialSummary_computesPreviousMonthMargin_asRevenueMinusCosts_forPreviousMonth() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());

        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of(
                monthlyProjection(previousMonth.toString(), new BigDecimal("5000.00"), new BigDecimal("3000.00")),
                monthlyProjection(currentMonth.toString(), new BigDecimal("9500.00"), new BigDecimal("6000.00"))));

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.previousMonthMargin()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getFinancialSummary_defaultsRevenueAndPreviousMargin_toZero_whenTrendRowMissingForMonth() {
        when(supplierInvoiceRepository.sumTotalByInvoiceDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(maintenanceRepository.sumCostByWorkshopEntryDateBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findUpcomingReceivables(any(), any())).thenReturn(List.of());
        when(supplierInvoiceRepository.findUpcomingPayables(any(), any())).thenReturn(List.of());
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        FinancialSummaryResponse summary = dashboardService().getFinancialSummary();

        assertThat(summary.monthlyRevenue()).isEqualByComparingTo("0");
        assertThat(summary.previousMonthMargin()).isEqualByComparingTo("0");
    }

    private Client client(String name) {
        Client client = new Client();
        client.setName(name);
        setId(client, "id");
        return client;
    }

    private Supplier supplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        setId(supplier, "id");
        return supplier;
    }

    private Invoice invoice(Client client, String invoiceNumber, LocalDate dueDate, BigDecimal total) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setDueDate(dueDate);
        invoice.setTotal(total);
        setId(invoice, "id");
        return invoice;
    }

    private SupplierInvoice supplierInvoice(Supplier supplier, String number, LocalDate dueDate, BigDecimal total) {
        SupplierInvoice supplierInvoice = new SupplierInvoice();
        supplierInvoice.setSupplier(supplier);
        supplierInvoice.setSupplierInvoiceNumber(number);
        supplierInvoice.setDueDate(dueDate);
        supplierInvoice.setTotal(total);
        setId(supplierInvoice, "id");
        return supplierInvoice;
    }

    // Both entities generate their UUID id via @GeneratedValue, which never runs on a plain `new`
    // instance in a unit test — reflection sets a deterministic id so UpcomingInvoiceResponse.id()
    // can be asserted without wiring up JPA here.
    private void setId(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

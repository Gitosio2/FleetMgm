package com.fleetmgm.dashboard.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.dashboard.application.DashboardService;
import com.fleetmgm.dashboard.dto.FinancialSummaryResponse;
import com.fleetmgm.dashboard.dto.FleetSummaryResponse;
import com.fleetmgm.dashboard.dto.UpcomingInvoiceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 403/role-based filtering (ADMIN/MANAGER/ADMINISTRATIVE only) is NOT tested here:
// @AutoConfigureMockMvc(addFilters = false) + a mocked DashboardService bypasses Spring Security's
// @PreAuthorize proxy entirely (same documented gap as AuditLogControllerTest/GpsControllerTest —
// no test in this codebase currently exercises @PreAuthorize's role check through the real AOP proxy).
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean DashboardService dashboardService;
    @Autowired MockMvc mockMvc;

    @Test
    void fleetSummary_returns200_withAssembledSummary() throws Exception {
        FleetSummaryResponse summary = new FleetSummaryResponse(12, 15, 2, 4, 1);
        when(dashboardService.getFleetSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/fleet-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeVehicles").value(12))
                .andExpect(jsonPath("$.totalVehicles").value(15))
                .andExpect(jsonPath("$.inWorkshop").value(2))
                .andExpect(jsonPath("$.pendingMaintenance").value(4))
                .andExpect(jsonPath("$.pendingMaintenanceDueSoon").value(1));
    }

    @Test
    void financialSummary_returns200_withAssembledSummary() throws Exception {
        UpcomingInvoiceResponse receivable = new UpcomingInvoiceResponse(
                UUID.randomUUID(), "INV-2026-00010", UUID.randomUUID(), "Acme Logistics",
                new BigDecimal("500.00"), LocalDate.now().plusDays(3), false);
        UpcomingInvoiceResponse payable = new UpcomingInvoiceResponse(
                UUID.randomUUID(), "F-2026-0456", UUID.randomUUID(), "Taller Mecánico Norte",
                new BigDecimal("121.00"), LocalDate.now().minusDays(1), true);
        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                new BigDecimal("8420.50"), new BigDecimal("9500.00"), new BigDecimal("2000.00"),
                List.of(receivable), List.of(payable));
        when(dashboardService.getFinancialSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/financial-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyCosts").value(8420.50))
                .andExpect(jsonPath("$.monthlyRevenue").value(9500.00))
                .andExpect(jsonPath("$.previousMonthMargin").value(2000.00))
                .andExpect(jsonPath("$.upcomingReceivables[0].number").value("INV-2026-00010"))
                .andExpect(jsonPath("$.upcomingReceivables[0].counterparty").value("Acme Logistics"))
                .andExpect(jsonPath("$.upcomingReceivables[0].overdue").value(false))
                .andExpect(jsonPath("$.upcomingPayables[0].number").value("F-2026-0456"))
                .andExpect(jsonPath("$.upcomingPayables[0].counterparty").value("Taller Mecánico Norte"))
                .andExpect(jsonPath("$.upcomingPayables[0].overdue").value(true));
    }
}

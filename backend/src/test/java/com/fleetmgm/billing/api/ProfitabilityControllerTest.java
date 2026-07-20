package com.fleetmgm.billing.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.billing.application.ProfitabilityService;
import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.dto.VehicleExpenseResponse;
import com.fleetmgm.billing.dto.VehicleRevenueLineItemResponse;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 403/role-based filtering (ADMIN/MANAGER/ADMINISTRATIVE only) is NOT tested here:
// @AutoConfigureMockMvc(addFilters = false) + a mocked ProfitabilityService bypasses Spring
// Security's @PreAuthorize proxy entirely (same documented gap as DashboardControllerTest/
// AuditLogControllerTest — no test in this codebase currently exercises @PreAuthorize's role
// check through the real AOP proxy).
@WebMvcTest(ProfitabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfitabilityControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean ProfitabilityService profitabilityService;
    @Autowired MockMvc mockMvc;

    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private ProfitabilityResponse sampleResponse() {
        return new ProfitabilityResponse(VEHICLE_ID, "1111AAA", "Toyota", "Hilux",
                new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("600.00"),
                null, null, UsageMeasure.KILOMETERS);
    }

    // --- GET /api/v1/reports/profitability ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<ProfitabilityResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(profitabilityService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/reports/profitability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- GET /api/v1/reports/profitability/{vehicleId} ---

    @Test
    void getByVehicleId_returns200_withProfitability() throws Exception {
        when(profitabilityService.getByVehicleId(eq(VEHICLE_ID), isNull(), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}", VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(VEHICLE_ID.toString()))
                .andExpect(jsonPath("$.vehicleLicensePlate").value("1111AAA"))
                .andExpect(jsonPath("$.revenue").value(1000.00))
                .andExpect(jsonPath("$.costs").value(400.00))
                .andExpect(jsonPath("$.margin").value(600.00));
    }

    @Test
    void getByVehicleId_forwardsFromAndToQueryParams() throws Exception {
        when(profitabilityService.getByVehicleId(
                VEHICLE_ID, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}", VEHICLE_ID)
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(1000.00));
    }

    @Test
    void getByVehicleId_returns404_whenVehicleMissing() throws Exception {
        when(profitabilityService.getByVehicleId(eq(VEHICLE_ID), isNull(), isNull()))
                .thenThrow(new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + VEHICLE_ID + " not found"));

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}", VEHICLE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // --- GET /api/v1/reports/profitability/{vehicleId}/revenue ---

    @Test
    void getRevenueByVehicle_returns200_withLineItems() throws Exception {
        VehicleRevenueLineItemResponse response = new VehicleRevenueLineItemResponse(
                "INV-2026-00001", LocalDate.of(2026, 7, 5), "Transport",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"));
        when(profitabilityService.getRevenueByVehicle(eq(VEHICLE_ID), isNull(), isNull()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/revenue", VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-2026-00001"))
                .andExpect(jsonPath("$[0].subtotal").value(100.00));
    }

    @Test
    void getRevenueByVehicle_forwardsFromAndToQueryParams() throws Exception {
        when(profitabilityService.getRevenueByVehicle(
                VEHICLE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/revenue", VEHICLE_ID)
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRevenueByVehicle_returns404_whenVehicleMissing() throws Exception {
        when(profitabilityService.getRevenueByVehicle(eq(VEHICLE_ID), isNull(), isNull()))
                .thenThrow(new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + VEHICLE_ID + " not found"));

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/revenue", VEHICLE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // --- GET /api/v1/reports/profitability/{vehicleId}/expenses ---

    @Test
    void getExpensesByVehicle_returns200_withExpenses() throws Exception {
        VehicleExpenseResponse response = new VehicleExpenseResponse(
                "Taller Central – F-2026-0456", LocalDate.of(2026, 7, 1), new BigDecimal("121.00"));
        when(profitabilityService.getExpensesByVehicle(eq(VEHICLE_ID), isNull(), isNull()))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/expenses", VEHICLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Taller Central – F-2026-0456"))
                .andExpect(jsonPath("$[0].amount").value(121.00));
    }

    @Test
    void getExpensesByVehicle_forwardsFromAndToQueryParams() throws Exception {
        when(profitabilityService.getExpensesByVehicle(
                VEHICLE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/expenses", VEHICLE_ID)
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getExpensesByVehicle_returns404_whenVehicleMissing() throws Exception {
        when(profitabilityService.getExpensesByVehicle(eq(VEHICLE_ID), isNull(), isNull()))
                .thenThrow(new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle " + VEHICLE_ID + " not found"));

        mockMvc.perform(get("/api/v1/reports/profitability/{vehicleId}/expenses", VEHICLE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // --- GET /api/v1/reports/profitability/trend ---

    @Test
    void financialTrend_returns200_withMonthlyData() throws Exception {
        MonthlyFinancialResponse response =
                new MonthlyFinancialResponse("2026-06", new BigDecimal("1000.00"), new BigDecimal("400.00"));
        when(profitabilityService.getFinancialTrend(3)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reports/profitability/trend").param("months", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-06"))
                .andExpect(jsonPath("$[0].revenue").value(1000.00))
                .andExpect(jsonPath("$[0].costs").value(400.00));
    }

    @Test
    void financialTrend_defaultsToSixMonths_whenNoParamProvided() throws Exception {
        when(profitabilityService.getFinancialTrend(6)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/profitability/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

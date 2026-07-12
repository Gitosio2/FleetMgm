package com.fleetmgm.gps.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.gps.application.GpsService;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 403/role-based filtering is NOT tested here: @AutoConfigureMockMvc(addFilters = false) + a
// mocked GpsService bypasses Spring Security's @PreAuthorize proxy entirely (same documented gap
// as SupplierInvoiceControllerTest). That behavior is covered by GpsServiceTest instead.
@WebMvcTest(GpsController.class)
@AutoConfigureMockMvc(addFilters = false)
class GpsControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean GpsService gpsService;
    @Autowired MockMvc mockMvc;

    @Test
    void latest_returns200_withPositions() throws Exception {
        GpsPositionResponse response = new GpsPositionResponse(UUID.randomUUID(), UUID.randomUUID(), "1234ABC",
                VehicleCategory.LIGHT_VEHICLE, 40.4168, -3.7038, 90.0, 50.0, Instant.now(), GpsSource.MOCK);
        when(gpsService.findLatest(isNull(), isNull())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/gps/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("1234ABC"));
    }

    @Test
    void latest_returns200_withEmptyList_whenNoPositionsRecorded() throws Exception {
        when(gpsService.findLatest(isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gps/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void latest_forwardsCategoryQueryParam_toService() throws Exception {
        when(gpsService.findLatest(eq(VehicleCategory.HEAVY_MACHINERY), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gps/latest").param("category", "HEAVY_MACHINERY"))
                .andExpect(status().isOk());
    }

    @Test
    void latest_forwardsVehicleIdQueryParam_toService() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(gpsService.findLatest(isNull(), eq(vehicleId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gps/latest").param("vehicleId", vehicleId.toString()))
                .andExpect(status().isOk());
    }
}

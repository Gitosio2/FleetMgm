package com.fleetmgm.gps.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.gps.application.GpsMockService;
import com.fleetmgm.gps.application.GpsService;
import com.fleetmgm.gps.domain.GpsSource;
import com.fleetmgm.gps.dto.GpsMockStatusResponse;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    @MockBean GpsMockService gpsMockService;
    @Autowired MockMvc mockMvc;

    @Test
    void latest_returns200_withPositions() throws Exception {
        GpsPositionResponse response = new GpsPositionResponse(UUID.randomUUID(), UUID.randomUUID(), "1234ABC",
                "Toyota", "Hilux", VehicleCategory.LIGHT_VEHICLE, 40.4168, -3.7038, 90.0, 50.0, Instant.now(), GpsSource.MOCK);
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

    @Test
    void mockStatus_returns200_withTheGeneratorState() throws Exception {
        Instant until = Instant.parse("2026-09-29T10:30:00Z");
        when(gpsMockService.status()).thenReturn(new GpsMockStatusResponse(true, until, 30));

        mockMvc.perform(get("/api/v1/gps/mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.enabledUntil").value("2026-09-29T10:30:00Z"))
                .andExpect(jsonPath("$.intervalSeconds").value(30));
    }

    @Test
    void updateMock_returns200_andForwardsTheRequestedState_toService() throws Exception {
        when(gpsMockService.setEnabled(true)).thenReturn(new GpsMockStatusResponse(true, null, 30));

        mockMvc.perform(patch("/api/v1/gps/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(gpsMockService).setEnabled(true);
    }

    @Test
    void updateMock_returns200_whenDisabling() throws Exception {
        when(gpsMockService.setEnabled(false)).thenReturn(new GpsMockStatusResponse(false, null, 30));

        mockMvc.perform(patch("/api/v1/gps/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.enabledUntil").isEmpty());
    }

    // A body with no "enabled" must not be read as "disable it" — see UpdateGpsMockRequest.
    @Test
    void updateMock_returns400_whenEnabledIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/gps/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(gpsMockService, never()).setEnabled(false);
    }
}

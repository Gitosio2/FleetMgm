package com.fleetmgm.workshop.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.workshop.application.MaintenanceService;
import com.fleetmgm.workshop.domain.MaintenanceCategory;
import com.fleetmgm.workshop.domain.MaintenanceStatus;
import com.fleetmgm.workshop.dto.MaintenanceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class MaintenanceControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean MaintenanceService maintenanceService;
    @Autowired MockMvc mockMvc;

    private static final UUID MAINTENANCE_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private MaintenanceResponse sampleResponse() {
        return new MaintenanceResponse(MAINTENANCE_ID, VEHICLE_ID, "1234ABC", "Toyota", "Corolla", "Oil change", null,
                null, null, null, null, null, null, MaintenanceStatus.SCHEDULED,
                MaintenanceCategory.PREVENTIVE, Instant.now(), null, null);
    }

    // --- GET /api/v1/maintenance ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<MaintenanceResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(maintenanceService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/maintenance ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(maintenanceService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"type\":\"Oil change\",\"scheduledDate\":\"2026-07-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/maintenance/")));
    }

    @Test
    void create_returns400_whenTypeMissing() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns400_whenVehicleIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"Oil change\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/v1/maintenance/{id} ---

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(maintenanceService.getById(MAINTENANCE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/maintenance/{id}", MAINTENANCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MAINTENANCE_ID.toString()))
                .andExpect(jsonPath("$.vehicleMake").value("Toyota"))
                .andExpect(jsonPath("$.vehicleModel").value("Corolla"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(maintenanceService.getById(MAINTENANCE_ID))
                .thenThrow(new NotFoundException("MAINTENANCE_NOT_FOUND", "Maintenance not found"));

        mockMvc.perform(get("/api/v1/maintenance/{id}", MAINTENANCE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_NOT_FOUND"));
    }

    // --- PUT /api/v1/maintenance/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        when(maintenanceService.update(eq(MAINTENANCE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/maintenance/{id}", MAINTENANCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"type\":\"Brake check\",\"category\":\"PREVENTIVE\"}"))
                .andExpect(status().isOk());
    }

    // --- DELETE /api/v1/maintenance/{id} ---

    @Test
    void delete_returns204_whenScheduled() throws Exception {
        doNothing().when(maintenanceService).delete(MAINTENANCE_ID);

        mockMvc.perform(delete("/api/v1/maintenance/{id}", MAINTENANCE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenNotScheduled() throws Exception {
        doThrow(new ConflictException("MAINTENANCE_DELETE_NOT_ALLOWED", "Cannot delete"))
                .when(maintenanceService).delete(MAINTENANCE_ID);

        mockMvc.perform(delete("/api/v1/maintenance/{id}", MAINTENANCE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_DELETE_NOT_ALLOWED"));
    }

    // --- PATCH /api/v1/maintenance/{id}/start ---

    @Test
    void start_returns200_withoutBody() throws Exception {
        when(maintenanceService.start(eq(MAINTENANCE_ID), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/maintenance/{id}/start", MAINTENANCE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void start_returns200_withUsageBody() throws Exception {
        when(maintenanceService.start(eq(MAINTENANCE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/maintenance/{id}/start", MAINTENANCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usageAtService\":15000}"))
                .andExpect(status().isOk());
    }

    @Test
    void start_returns409_whenInvalidTransition() throws Exception {
        when(maintenanceService.start(eq(MAINTENANCE_ID), isNull()))
                .thenThrow(new ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION", "Cannot start"));

        mockMvc.perform(patch("/api/v1/maintenance/{id}/start", MAINTENANCE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_INVALID_STATE_TRANSITION"));
    }

    // --- PATCH /api/v1/maintenance/{id}/complete ---

    @Test
    void complete_returns200_withCostBody() throws Exception {
        when(maintenanceService.complete(eq(MAINTENANCE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/maintenance/{id}/complete", MAINTENANCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cost\":250.00}"))
                .andExpect(status().isOk());
    }

    // --- PATCH /api/v1/maintenance/{id}/cancel ---

    @Test
    void cancel_returns200_withoutBody() throws Exception {
        when(maintenanceService.cancel(MAINTENANCE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/maintenance/{id}/cancel", MAINTENANCE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returns404_whenMissing() throws Exception {
        when(maintenanceService.cancel(MAINTENANCE_ID))
                .thenThrow(new NotFoundException("MAINTENANCE_NOT_FOUND", "Maintenance not found"));

        mockMvc.perform(patch("/api/v1/maintenance/{id}/cancel", MAINTENANCE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_NOT_FOUND"));
    }

    @Test
    void cancel_returns409_whenInvalidTransition() throws Exception {
        when(maintenanceService.cancel(MAINTENANCE_ID))
                .thenThrow(new ConflictException("MAINTENANCE_INVALID_STATE_TRANSITION", "Cannot cancel"));

        mockMvc.perform(patch("/api/v1/maintenance/{id}/cancel", MAINTENANCE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAINTENANCE_INVALID_STATE_TRANSITION"));
    }
}

package com.fleetmgm.workshop.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.workshop.application.WorkshopScheduleService;
import com.fleetmgm.workshop.domain.SchedulePriority;
import com.fleetmgm.workshop.domain.WorkshopStatus;
import com.fleetmgm.workshop.dto.ScheduleResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkshopController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkshopControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean WorkshopScheduleService workshopScheduleService;
    @Autowired MockMvc mockMvc;

    private static final UUID SCHEDULE_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private ScheduleResponse sampleResponse() {
        return new ScheduleResponse(SCHEDULE_ID, VEHICLE_ID, "1234ABC", "Toyota", "Corolla", null, null, null, null,
                LocalDate.now(), "Oil change", SchedulePriority.MEDIUM, WorkshopStatus.PENDING, null, null, null, null);
    }

    // --- GET /api/v1/workshop/schedules ---

    @Test
    void list_returns200_withPage_whenRangeValid() throws Exception {
        PageResponse<ScheduleResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(workshopScheduleService.listByRange(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/workshop/schedules").param("range", "today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_defaultsToNewestScheduledFirst() throws Exception {
        PageResponse<ScheduleResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(workshopScheduleService.listByRange(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/workshop/schedules").param("range", "today")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(workshopScheduleService).listByRange(any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("scheduledDate");
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void list_returns400_whenRangeInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/workshop/schedules").param("range", "yesterday"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));
    }

    @Test
    void list_returns400_whenRangeMissing() throws Exception {
        mockMvc.perform(get("/api/v1/workshop/schedules"))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/v1/workshop/schedules ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(workshopScheduleService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/workshop/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"scheduledDate\":\"2026-08-01\",\"type\":\"Oil change\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/workshop/schedules/")));
    }

    @Test
    void create_returns400_whenVehicleIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/workshop/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledDate\":\"2026-08-01\",\"type\":\"Oil change\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns400_whenTypeMissing() throws Exception {
        mockMvc.perform(post("/api/v1/workshop/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"scheduledDate\":\"2026-08-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns404_whenVehicleMissing() throws Exception {
        when(workshopScheduleService.create(any()))
                .thenThrow(new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found"));

        mockMvc.perform(post("/api/v1/workshop/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"scheduledDate\":\"2026-08-01\",\"type\":\"Oil change\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // --- GET /api/v1/workshop/schedules/{id} ---

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(workshopScheduleService.getById(SCHEDULE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/workshop/schedules/{id}", SCHEDULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SCHEDULE_ID.toString()))
                .andExpect(jsonPath("$.vehicleMake").value("Toyota"))
                .andExpect(jsonPath("$.vehicleModel").value("Corolla"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(workshopScheduleService.getById(SCHEDULE_ID))
                .thenThrow(new NotFoundException("SCHEDULE_NOT_FOUND", "Schedule not found"));

        mockMvc.perform(get("/api/v1/workshop/schedules/{id}", SCHEDULE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));
    }

    // --- PUT /api/v1/workshop/schedules/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        when(workshopScheduleService.update(eq(SCHEDULE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/workshop/schedules/{id}", SCHEDULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"scheduledDate\":\"2026-08-01\","
                                + "\"type\":\"Brake check\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_returns400_whenPriorityMissing() throws Exception {
        mockMvc.perform(put("/api/v1/workshop/schedules/{id}", SCHEDULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"scheduledDate\":\"2026-08-01\",\"type\":\"Brake check\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- DELETE /api/v1/workshop/schedules/{id} ---

    @Test
    void delete_returns204_whenPending() throws Exception {
        doNothing().when(workshopScheduleService).delete(SCHEDULE_ID);

        mockMvc.perform(delete("/api/v1/workshop/schedules/{id}", SCHEDULE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenNotPending() throws Exception {
        doThrow(new ConflictException("SCHEDULE_DELETE_NOT_ALLOWED", "Cannot delete"))
                .when(workshopScheduleService).delete(SCHEDULE_ID);

        mockMvc.perform(delete("/api/v1/workshop/schedules/{id}", SCHEDULE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_DELETE_NOT_ALLOWED"));
    }

    // --- PATCH /api/v1/workshop/schedules/{id}/start ---

    @Test
    void start_returns200_whenPending() throws Exception {
        when(workshopScheduleService.start(SCHEDULE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/workshop/schedules/{id}/start", SCHEDULE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void start_returns409_whenInvalidTransition() throws Exception {
        when(workshopScheduleService.start(SCHEDULE_ID))
                .thenThrow(new ConflictException("SCHEDULE_INVALID_STATE_TRANSITION", "Cannot start"));

        mockMvc.perform(patch("/api/v1/workshop/schedules/{id}/start", SCHEDULE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_INVALID_STATE_TRANSITION"));
    }

    // --- PATCH /api/v1/workshop/schedules/{id}/cancel ---

    @Test
    void cancel_returns200_whenPendingOrInProgress() throws Exception {
        when(workshopScheduleService.cancel(SCHEDULE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/workshop/schedules/{id}/cancel", SCHEDULE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returns409_whenInvalidTransition() throws Exception {
        when(workshopScheduleService.cancel(SCHEDULE_ID))
                .thenThrow(new ConflictException("SCHEDULE_INVALID_STATE_TRANSITION", "Cannot cancel"));

        mockMvc.perform(patch("/api/v1/workshop/schedules/{id}/cancel", SCHEDULE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_INVALID_STATE_TRANSITION"));
    }
}

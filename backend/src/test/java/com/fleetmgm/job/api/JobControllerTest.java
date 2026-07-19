package com.fleetmgm.job.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.job.application.JobService;
import com.fleetmgm.job.domain.JobStatus;
import com.fleetmgm.job.dto.JobResponse;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean JobService jobService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private static final Instant ACTUAL_START = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant ACTUAL_END = Instant.parse("2026-01-01T12:00:00Z");

    private JobResponse sampleResponse() {
        return new JobResponse(JOB_ID, "Delivery", "desc", VEHICLE_ID, "1234ABC", "Toyota", "Corolla",
                null, null, null, null, JobStatus.PENDING,
                "Origin", "Destination", null, null, null, null, null, null, null, null, Instant.now());
    }

    private JobResponse sampleResponseWithActualDates() {
        return new JobResponse(JOB_ID, "Delivery", "desc", VEHICLE_ID, "1234ABC", "Toyota", "Corolla",
                null, null, null, null, JobStatus.PENDING,
                "Origin", "Destination", null, null, null, ACTUAL_START, ACTUAL_END, null, null, null, Instant.now());
    }

    // --- GET /api/v1/jobs ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<JobResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(jobService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_passesAllFilters_toService() throws Exception {
        PageResponse<JobResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        UUID vehicleId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        when(jobService.list(eq("Entrega"), eq("Almacén"), eq("Cliente"), eq(vehicleId), eq(driverId),
                eq(JobStatus.PENDING), eq(ACTUAL_START), eq(ACTUAL_END), eq(ACTUAL_START), eq(ACTUAL_END),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/jobs")
                        .param("title", "Entrega")
                        .param("originLocation", "Almacén")
                        .param("destinationLocation", "Cliente")
                        .param("vehicleId", vehicleId.toString())
                        .param("assignedDriverId", driverId.toString())
                        .param("status", "PENDING")
                        .param("actualStartFrom", ACTUAL_START.toString())
                        .param("actualStartTo", ACTUAL_END.toString())
                        .param("actualEndFrom", ACTUAL_START.toString())
                        .param("actualEndTo", ACTUAL_END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/jobs ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(jobService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"title\":\"Delivery\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/jobs/")));
    }

    @Test
    void create_returns201_withActualDates_whenProvided() throws Exception {
        when(jobService.create(any())).thenReturn(sampleResponseWithActualDates());

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"title\":\"Delivery\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\","
                                + "\"actualStart\":\"" + ACTUAL_START + "\",\"actualEnd\":\"" + ACTUAL_END + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actualStart").value(ACTUAL_START.toString()))
                .andExpect(jsonPath("$.actualEnd").value(ACTUAL_END.toString()));
    }

    @Test
    void create_returns400_whenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns400_whenVehicleIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Delivery\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/v1/jobs/{id} ---

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(jobService.getById(JOB_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/jobs/{id}", JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                .andExpect(jsonPath("$.vehicleMake").value("Toyota"))
                .andExpect(jsonPath("$.vehicleModel").value("Corolla"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(jobService.getById(JOB_ID))
                .thenThrow(new NotFoundException("JOB_NOT_FOUND", "Job not found"));

        mockMvc.perform(get("/api/v1/jobs/{id}", JOB_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
    }

    // --- PUT /api/v1/jobs/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        when(jobService.update(eq(JOB_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/jobs/{id}", JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"title\":\"Delivery\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_returns200_withActualDates_whenProvided() throws Exception {
        when(jobService.update(eq(JOB_ID), any())).thenReturn(sampleResponseWithActualDates());

        mockMvc.perform(put("/api/v1/jobs/{id}", JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + VEHICLE_ID + "\",\"title\":\"Delivery\","
                                + "\"originLocation\":\"Origin\",\"destinationLocation\":\"Destination\","
                                + "\"actualStart\":\"" + ACTUAL_START + "\",\"actualEnd\":\"" + ACTUAL_END + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualStart").value(ACTUAL_START.toString()))
                .andExpect(jsonPath("$.actualEnd").value(ACTUAL_END.toString()));
    }

    // --- DELETE /api/v1/jobs/{id} ---

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(jobService).delete(JOB_ID);

        mockMvc.perform(delete("/api/v1/jobs/{id}", JOB_ID))
                .andExpect(status().isNoContent());
    }

    // --- PATCH /api/v1/jobs/{id}/start ---

    @Test
    void start_returns200_withoutBody() throws Exception {
        when(jobService.start(eq(JOB_ID), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/jobs/{id}/start", JOB_ID))
                .andExpect(status().isOk());
    }

    @Test
    void start_returns200_withUsageValueBody() throws Exception {
        when(jobService.start(eq(JOB_ID), eq(1000L))).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/jobs/{id}/start", JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startUsageValue\":1000}"))
                .andExpect(status().isOk());
    }

    @Test
    void start_returns409_whenInvalidTransition() throws Exception {
        when(jobService.start(eq(JOB_ID), isNull()))
                .thenThrow(new ConflictException("JOB_INVALID_STATE_TRANSITION", "Job cannot be started"));

        mockMvc.perform(patch("/api/v1/jobs/{id}/start", JOB_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_INVALID_STATE_TRANSITION"));
    }

    // --- PATCH /api/v1/jobs/{id}/complete ---

    @Test
    void complete_returns200_withoutBody() throws Exception {
        when(jobService.complete(eq(JOB_ID), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/jobs/{id}/complete", JOB_ID))
                .andExpect(status().isOk());
    }

    @Test
    void complete_returns409_whenInvalidTransition() throws Exception {
        when(jobService.complete(eq(JOB_ID), isNull()))
                .thenThrow(new ConflictException("JOB_INVALID_STATE_TRANSITION", "Job cannot be completed"));

        mockMvc.perform(patch("/api/v1/jobs/{id}/complete", JOB_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_INVALID_STATE_TRANSITION"));
    }

    // --- PATCH /api/v1/jobs/{id}/cancel ---

    @Test
    void cancel_returns200_whenValidTransition() throws Exception {
        when(jobService.cancel(JOB_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/jobs/{id}/cancel", JOB_ID))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returns409_whenInvalidTransition() throws Exception {
        when(jobService.cancel(JOB_ID))
                .thenThrow(new ConflictException("JOB_INVALID_STATE_TRANSITION", "Job cannot be cancelled"));

        mockMvc.perform(patch("/api/v1/jobs/{id}/cancel", JOB_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_INVALID_STATE_TRANSITION"));
    }
}

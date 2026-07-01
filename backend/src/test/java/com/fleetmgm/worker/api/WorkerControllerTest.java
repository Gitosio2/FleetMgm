package com.fleetmgm.worker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.worker.application.WorkerService;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.worker.dto.CreateWorkerRequest;
import com.fleetmgm.worker.dto.WorkerResponse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkerController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkerControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean WorkerService workerService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID WORKER_ID = UUID.randomUUID();

    private WorkerResponse sampleResponse() {
        return new WorkerResponse(WORKER_ID, "Juan", "García", "Juan García",
                WorkerRole.DRIVER, "12345678A", null, null, null, null, Instant.now());
    }

    // --- GET /api/v1/workers ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<WorkerResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(workerService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/workers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/workers ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(workerService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Juan\",\"lastName\":\"García\",\"workerRole\":\"DRIVER\",\"nationalId\":\"12345678A\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/workers/")));
    }

    @Test
    void create_returns400_whenFirstNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"García\",\"workerRole\":\"DRIVER\",\"nationalId\":\"12345678A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns409_whenNationalIdDuplicated() throws Exception {
        when(workerService.create(any()))
                .thenThrow(new ConflictException("WORKER_NATIONAL_ID_CONFLICT", "National ID already in use"));

        mockMvc.perform(post("/api/v1/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Juan\",\"lastName\":\"García\",\"workerRole\":\"DRIVER\",\"nationalId\":\"12345678A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKER_NATIONAL_ID_CONFLICT"));
    }

    // --- GET /api/v1/workers/{id} ---

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(workerService.getById(WORKER_ID))
                .thenThrow(new NotFoundException("WORKER_NOT_FOUND", "Worker not found"));

        mockMvc.perform(get("/api/v1/workers/{id}", WORKER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_FOUND"));
    }

    // --- DELETE /api/v1/workers/{id} ---

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(workerService).delete(WORKER_ID);

        mockMvc.perform(delete("/api/v1/workers/{id}", WORKER_ID))
                .andExpect(status().isNoContent());
    }
}

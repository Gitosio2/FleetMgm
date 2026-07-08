package com.fleetmgm.vehicle.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.application.AssignmentService;
import com.fleetmgm.vehicle.dto.AssignmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssignmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssignmentControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean AssignmentService assignmentService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    private AssignmentResponse sampleResponse() {
        return new AssignmentResponse(ASSIGNMENT_ID, UUID.randomUUID(), "Juan García",
                UUID.randomUUID(), "1234ABC", "Toyota", "Corolla", LocalDate.now(), null, UUID.randomUUID(),
                "notes", Instant.now(), true);
    }

    // --- POST /api/v1/assignments ---

    @Test
    void assign_returns201_withLocation_whenValid() throws Exception {
        when(assignmentService.assign(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + UUID.randomUUID() + "\",\"vehicleId\":\"" + UUID.randomUUID()
                                + "\",\"startDate\":\"2026-07-06\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/assignments/")));
    }

    @Test
    void assign_returns400_whenDriverIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":\"" + UUID.randomUUID() + "\",\"startDate\":\"2026-07-06\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void assign_returns409_whenDriverAlreadyActive() throws Exception {
        when(assignmentService.assign(any()))
                .thenThrow(new ConflictException("ASSIGNMENT_DRIVER_ALREADY_ACTIVE", "Driver already has an active assignment"));

        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + UUID.randomUUID() + "\",\"vehicleId\":\"" + UUID.randomUUID()
                                + "\",\"startDate\":\"2026-07-06\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_DRIVER_ALREADY_ACTIVE"));
    }

    // --- PATCH /api/v1/assignments/{id}/end ---

    @Test
    void endAssignment_returns200_whenExists() throws Exception {
        when(assignmentService.endAssignment(ASSIGNMENT_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/assignments/{id}/end", ASSIGNMENT_ID))
                .andExpect(status().isOk());
    }

    @Test
    void endAssignment_returns404_whenMissing() throws Exception {
        when(assignmentService.endAssignment(ASSIGNMENT_ID))
                .thenThrow(new NotFoundException("ASSIGNMENT_NOT_FOUND", "Assignment not found"));

        mockMvc.perform(patch("/api/v1/assignments/{id}/end", ASSIGNMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_NOT_FOUND"));
    }

    // --- GET /api/v1/workers/{workerId}/assignments ---

    @Test
    void historyByWorker_returns200_withPage() throws Exception {
        UUID workerId = UUID.randomUUID();
        PageResponse<AssignmentResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(assignmentService.historyByWorker(eq(workerId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/workers/{workerId}/assignments", workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- GET /api/v1/vehicles/{vehicleId}/assignment ---

    @Test
    void activeByVehicle_returns200_withBody_whenPresent() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(assignmentService.activeByVehicle(vehicleId)).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/assignment", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ASSIGNMENT_ID.toString()))
                .andExpect(jsonPath("$.vehicleMake").value("Toyota"))
                .andExpect(jsonPath("$.vehicleModel").value("Corolla"));
    }

    @Test
    void activeByVehicle_returns204_whenAbsent() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        when(assignmentService.activeByVehicle(vehicleId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/vehicles/{vehicleId}/assignment", vehicleId))
                .andExpect(status().isNoContent());
    }

    // --- GET /api/v1/assignments/active ---

    @Test
    void activeByDrivers_returns200_withCommaSeparatedDriverIdsBound() throws Exception {
        UUID driverId1 = UUID.randomUUID();
        UUID driverId2 = UUID.randomUUID();
        when(assignmentService.activeByDrivers(List.of(driverId1, driverId2)))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/assignments/active?driverIds=" + driverId1 + "," + driverId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void activeByDrivers_returns200_withEmptyArray_whenNoActiveAssignments() throws Exception {
        UUID driverId = UUID.randomUUID();
        when(assignmentService.activeByDrivers(List.of(driverId))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assignments/active?driverIds=" + driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

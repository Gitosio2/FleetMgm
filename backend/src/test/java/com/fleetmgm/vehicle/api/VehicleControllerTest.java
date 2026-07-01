package com.fleetmgm.vehicle.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.application.VehicleService;
import com.fleetmgm.vehicle.domain.AcquisitionType;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.VehicleStatus;
import com.fleetmgm.vehicle.dto.VehicleResponse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean VehicleService vehicleService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID VEHICLE_ID = UUID.randomUUID();

    private VehicleResponse sampleResponse() {
        return new VehicleResponse(VEHICLE_ID,
                VehicleCategory.LIGHT_VEHICLE, UsageMeasure.KILOMETERS,
                "Toyota", "Corolla", 2020,
                "1234ABC", null, null, null,
                VehicleStatus.ACTIVE,
                null, null, null, null, null, null, null, null,
                Instant.now());
    }

    // --- GET /api/v1/vehicles ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<VehicleResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(vehicleService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/vehicles ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(vehicleService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCategory\":\"LIGHT_VEHICLE\",\"usageMeasure\":\"KILOMETERS\",\"make\":\"Toyota\",\"model\":\"Corolla\",\"year\":2020}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/vehicles/")));
    }

    @Test
    void create_returns400_whenMakeBlank() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCategory\":\"LIGHT_VEHICLE\",\"usageMeasure\":\"KILOMETERS\",\"make\":\"\",\"model\":\"Corolla\",\"year\":2020}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns409_whenLicensePlateDuplicated() throws Exception {
        when(vehicleService.create(any()))
                .thenThrow(new ConflictException("VEHICLE_LICENSE_PLATE_CONFLICT", "License plate already in use"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCategory\":\"LIGHT_VEHICLE\",\"usageMeasure\":\"KILOMETERS\",\"make\":\"Toyota\",\"model\":\"Corolla\",\"year\":2020,\"licensePlate\":\"1234ABC\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_LICENSE_PLATE_CONFLICT"));
    }

    // --- GET /api/v1/vehicles/{id} ---

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(vehicleService.getById(VEHICLE_ID))
                .thenThrow(new NotFoundException("VEHICLE_NOT_FOUND", "Vehicle not found"));

        mockMvc.perform(get("/api/v1/vehicles/{id}", VEHICLE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
    }

    // --- DELETE /api/v1/vehicles/{id} ---

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(vehicleService).delete(VEHICLE_ID);

        mockMvc.perform(delete("/api/v1/vehicles/{id}", VEHICLE_ID))
                .andExpect(status().isNoContent());
    }
}

package com.fleetmgm.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.client.application.ClientService;
import com.fleetmgm.client.dto.ClientResponse;
import com.fleetmgm.client.dto.CreateClientRequest;
import com.fleetmgm.client.dto.UpdateClientRequest;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean ClientService clientService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID CLIENT_ID = UUID.randomUUID();

    private ClientResponse sampleResponse() {
        return new ClientResponse(CLIENT_ID, "Acme", "B12345678", "acme@example.com", null, null, Instant.now());
    }

    // --- GET /api/v1/clients ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<ClientResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(clientService.list(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_passesNameAndTaxIdFilters_toService() throws Exception {
        PageResponse<ClientResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(clientService.list(eq("Acme"), eq("B123"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/clients").param("name", "Acme").param("taxId", "B123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/clients ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        CreateClientRequest request = new CreateClientRequest("Acme", "B12345678", null, null, null);
        when(clientService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/clients/")));
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"taxId\":\"B12345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns409_whenTaxIdDuplicated() throws Exception {
        CreateClientRequest request = new CreateClientRequest("Acme", "B12345678", null, null, null);
        when(clientService.create(any()))
                .thenThrow(new ConflictException("CLIENT_TAX_ID_CONFLICT", "taxId already exists"));

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_TAX_ID_CONFLICT"));
    }

    // --- GET /api/v1/clients/{id} ---

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(clientService.getById(CLIENT_ID))
                .thenThrow(new NotFoundException("CLIENT_NOT_FOUND", "Client not found"));

        mockMvc.perform(get("/api/v1/clients/{id}", CLIENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    // --- PUT /api/v1/clients/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        UpdateClientRequest request = new UpdateClientRequest("Acme Updated", "B12345678", null, null, null);
        when(clientService.update(eq(CLIENT_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/clients/{id}", CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CLIENT_ID.toString()));
    }

    // --- DELETE /api/v1/clients/{id} ---

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(clientService).delete(CLIENT_ID);

        mockMvc.perform(delete("/api/v1/clients/{id}", CLIENT_ID))
                .andExpect(status().isNoContent());
    }
}

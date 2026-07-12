package com.fleetmgm.supplier.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.supplier.application.SupplierService;
import com.fleetmgm.supplier.dto.CreateSupplierRequest;
import com.fleetmgm.supplier.dto.SupplierResponse;
import com.fleetmgm.supplier.dto.UpdateSupplierRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean SupplierService supplierService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static final UUID SUPPLIER_ID = UUID.randomUUID();

    private SupplierResponse sampleResponse() {
        return new SupplierResponse(SUPPLIER_ID, "Acme Parts", "B12345678", "acme@example.com", null, null, Instant.now());
    }

    // --- GET /api/v1/suppliers ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<SupplierResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(supplierService.list(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/suppliers ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", "B12345678", null, null, null);
        when(supplierService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/suppliers/")));
    }

    @Test
    void create_returns201_withValid_whenTaxIdOmitted() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", null, null, null, null);
        when(supplierService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_returns409_whenTaxIdDuplicated() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", "B12345678", null, null, null);
        when(supplierService.create(any()))
                .thenThrow(new ConflictException("SUPPLIER_TAX_ID_CONFLICT", "taxId already exists"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPLIER_TAX_ID_CONFLICT"));
    }

    // --- GET /api/v1/suppliers/{id} ---

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(supplierService.getById(SUPPLIER_ID))
                .thenThrow(new NotFoundException("SUPPLIER_NOT_FOUND", "Supplier not found"));

        mockMvc.perform(get("/api/v1/suppliers/{id}", SUPPLIER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPLIER_NOT_FOUND"));
    }

    // --- PUT /api/v1/suppliers/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        UpdateSupplierRequest request = new UpdateSupplierRequest("Acme Updated", "B12345678", null, null, null);
        when(supplierService.update(eq(SUPPLIER_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/suppliers/{id}", SUPPLIER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUPPLIER_ID.toString()));
    }

    // --- DELETE /api/v1/suppliers/{id} ---

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(supplierService).delete(SUPPLIER_ID);

        mockMvc.perform(delete("/api/v1/suppliers/{id}", SUPPLIER_ID))
                .andExpect(status().isNoContent());
    }
}

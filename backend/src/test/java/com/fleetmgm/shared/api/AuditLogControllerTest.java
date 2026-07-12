package com.fleetmgm.shared.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.application.AuditLogService;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.dto.AuditLogResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 403/role-based filtering (ADMIN/MANAGER only) is NOT tested here: @AutoConfigureMockMvc(addFilters
// = false) + a mocked AuditLogService bypasses Spring Security's @PreAuthorize proxy entirely (same
// documented gap as GpsControllerTest/SupplierInvoiceControllerTest). No test in this codebase
// currently exercises @PreAuthorize's role check through the real AOP proxy.
@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean AuditLogService auditLogService;
    @Autowired MockMvc mockMvc;

    private AuditLogResponse sampleResponse() {
        return new AuditLogResponse(UUID.randomUUID(), "VEHICLE", UUID.randomUUID().toString(),
                AuditAction.UPDATE, UUID.randomUUID(), "admin@example.com", Instant.now(),
                "127.0.0.1", null, null, null);
    }

    // --- GET /api/v1/audit ---

    @Test
    void list_returns200_withoutFilters() throws Exception {
        PageResponse<AuditLogResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(auditLogService.list(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_returns200_filteredByEntityType() throws Exception {
        PageResponse<AuditLogResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(auditLogService.list(eq("VEHICLE"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit").param("entityType", "VEHICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("VEHICLE"));
    }

    @Test
    void list_returns200_filteredByAction() throws Exception {
        PageResponse<AuditLogResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(auditLogService.list(isNull(), eq(AuditAction.UPDATE), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit").param("action", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("UPDATE"));
    }

    @Test
    void list_returns200_filteredByDateRange() throws Exception {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-12T23:59:59Z");
        PageResponse<AuditLogResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(auditLogService.list(isNull(), isNull(), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/audit")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_returns400_whenActionIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/audit").param("action", "NOT_A_REAL_ACTION"))
                .andExpect(status().isBadRequest());
    }
}

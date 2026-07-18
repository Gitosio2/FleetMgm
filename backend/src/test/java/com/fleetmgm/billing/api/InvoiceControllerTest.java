package com.fleetmgm.billing.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.billing.application.InvoiceService;
import com.fleetmgm.billing.application.PdfExportService;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean InvoiceService invoiceService;
    @MockBean PdfExportService pdfExportService;
    @Autowired MockMvc mockMvc;

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID LINE_ITEM_ID = UUID.randomUUID();

    private InvoiceResponse sampleResponse() {
        return new InvoiceResponse(INVOICE_ID, "INV-2026-00001", CLIENT_ID, "Acme Corp", InvoiceStatus.DRAFT,
                null, null, null, new BigDecimal("0.2100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, Instant.now(), List.of());
    }

    // --- GET /api/v1/invoices ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<InvoiceResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_forwardsQueryParams_toService() throws Exception {
        PageResponse<InvoiceResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);

        when(invoiceService.list(
                eq(CLIENT_ID), eq("INV-2026-00"), eq(InvoiceStatus.ISSUED),
                eq(LocalDate.parse("2026-01-01")), eq(LocalDate.parse("2026-12-31")),
                eq(LocalDate.parse("2026-02-01")), eq(LocalDate.parse("2026-11-30")),
                eq(new BigDecimal("50")), eq(new BigDecimal("200")), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices")
                        .param("clientId", CLIENT_ID.toString())
                        .param("invoiceNumber", "INV-2026-00")
                        .param("status", "ISSUED")
                        .param("issueDateFrom", "2026-01-01")
                        .param("issueDateTo", "2026-12-31")
                        .param("dueDateFrom", "2026-02-01")
                        .param("dueDateTo", "2026-11-30")
                        .param("totalMin", "50")
                        .param("totalMax", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/invoices ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(invoiceService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/invoices/")));
    }

    @Test
    void create_returns400_whenClientIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/v1/invoices/{id} ---

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(invoiceService.getById(INVOICE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2026-00001"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(invoiceService.getById(INVOICE_ID))
                .thenThrow(new NotFoundException("INVOICE_NOT_FOUND", "Invoice not found"));

        mockMvc.perform(get("/api/v1/invoices/{id}", INVOICE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"));
    }

    // --- PUT /api/v1/invoices/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        when(invoiceService.update(eq(INVOICE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/invoices/{id}", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_returns409_whenNotDraft() throws Exception {
        when(invoiceService.update(eq(INVOICE_ID), any()))
                .thenThrow(new ConflictException("INVOICE_INVALID_STATE_TRANSITION", "Cannot update"));

        mockMvc.perform(put("/api/v1/invoices/{id}", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"" + CLIENT_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_INVALID_STATE_TRANSITION"));
    }

    // --- DELETE /api/v1/invoices/{id} ---

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(invoiceService).delete(INVOICE_ID);

        mockMvc.perform(delete("/api/v1/invoices/{id}", INVOICE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenNotDraft() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("INVOICE_DELETE_NOT_ALLOWED", "Cannot delete"))
                .when(invoiceService).delete(INVOICE_ID);

        mockMvc.perform(delete("/api/v1/invoices/{id}", INVOICE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_DELETE_NOT_ALLOWED"));
    }

    // --- PATCH /api/v1/invoices/{id}/issue ---

    @Test
    void issue_returns200_whenValid() throws Exception {
        when(invoiceService.issue(INVOICE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/invoices/{id}/issue", INVOICE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void issue_returns409_whenNoLineItems() throws Exception {
        when(invoiceService.issue(INVOICE_ID))
                .thenThrow(new ConflictException("INVOICE_NO_LINE_ITEMS", "No line items"));

        mockMvc.perform(patch("/api/v1/invoices/{id}/issue", INVOICE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NO_LINE_ITEMS"));
    }

    // --- PATCH /api/v1/invoices/{id}/pay ---

    @Test
    void pay_returns200_withoutBody() throws Exception {
        when(invoiceService.pay(eq(INVOICE_ID), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/invoices/{id}/pay", INVOICE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void pay_returns200_withPaymentDateBody() throws Exception {
        when(invoiceService.pay(eq(INVOICE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/invoices/{id}/pay", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\":\"2026-07-01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void pay_returns409_whenNotIssued() throws Exception {
        when(invoiceService.pay(eq(INVOICE_ID), isNull()))
                .thenThrow(new ConflictException("INVOICE_INVALID_STATE_TRANSITION", "Cannot pay"));

        mockMvc.perform(patch("/api/v1/invoices/{id}/pay", INVOICE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_INVALID_STATE_TRANSITION"));
    }

    // --- POST /api/v1/invoices/{id}/line-items ---

    @Test
    void addLineItem_returns201_whenValid() throws Exception {
        LineItemResponse response = new LineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null);
        when(invoiceService.addLineItem(eq(INVOICE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isCreated());
    }

    @Test
    void addLineItem_returns400_whenDescriptionMissing() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void addLineItem_returns404_whenInvoiceMissing() throws Exception {
        when(invoiceService.addLineItem(eq(INVOICE_ID), any()))
                .thenThrow(new NotFoundException("INVOICE_NOT_FOUND", "Invoice not found"));

        mockMvc.perform(post("/api/v1/invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"));
    }

    // --- PATCH /api/v1/invoices/{id}/line-items/{lineItemId} ---

    @Test
    void updateLineItem_returns200_whenValid() throws Exception {
        LineItemResponse response = new LineItemResponse(LINE_ITEM_ID, "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null);
        when(invoiceService.updateLineItem(eq(INVOICE_ID), eq(LINE_ITEM_ID), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/invoices/{id}/line-items/{lineItemId}", INVOICE_ID, LINE_ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateLineItem_returns400_whenDescriptionMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/invoices/{id}/line-items/{lineItemId}", INVOICE_ID, LINE_ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateLineItem_returns404_whenLineItemMissing() throws Exception {
        when(invoiceService.updateLineItem(eq(INVOICE_ID), eq(LINE_ITEM_ID), any()))
                .thenThrow(new NotFoundException("LINE_ITEM_NOT_FOUND", "Line item not found"));

        mockMvc.perform(patch("/api/v1/invoices/{id}/line-items/{lineItemId}", INVOICE_ID, LINE_ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINE_ITEM_NOT_FOUND"));
    }
}

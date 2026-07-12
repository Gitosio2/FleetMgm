package com.fleetmgm.billing.api;

import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.billing.application.SupplierInvoiceService;
import com.fleetmgm.billing.domain.ExpenseCategory;
import com.fleetmgm.billing.domain.SupplierInvoiceStatus;
import com.fleetmgm.billing.dto.SupplierInvoiceResponse;
import com.fleetmgm.billing.dto.SupplierLineItemResponse;
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

@WebMvcTest(SupplierInvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierInvoiceControllerTest {

    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean SupplierInvoiceService supplierInvoiceService;
    @Autowired MockMvc mockMvc;

    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID SUPPLIER_ID = UUID.randomUUID();

    private SupplierInvoiceResponse sampleResponse() {
        return new SupplierInvoiceResponse(INVOICE_ID, SUPPLIER_ID, "Acme Parts", "SUP-001", ExpenseCategory.MAINTENANCE,
                LocalDate.now(), null, null, SupplierInvoiceStatus.PENDING,
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("121.00"),
                null, null, null, null, null, null, Instant.now());
    }

    // --- GET /api/v1/supplier-invoices ---

    @Test
    void list_returns200_withPage() throws Exception {
        PageResponse<SupplierInvoiceResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(supplierInvoiceService.list(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/supplier-invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /api/v1/supplier-invoices ---

    @Test
    void create_returns201_withLocation_whenValid() throws Exception {
        when(supplierInvoiceService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/supplier-invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\"" + SUPPLIER_ID + "\",\"category\":\"MAINTENANCE\","
                                + "\"invoiceDate\":\"2026-07-01\",\"subtotal\":100.00,\"taxAmount\":21.00,\"total\":121.00}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/supplier-invoices/")));
    }

    @Test
    void create_returns400_whenSupplierIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/supplier-invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"MAINTENANCE\",\"invoiceDate\":\"2026-07-01\","
                                + "\"subtotal\":100.00,\"taxAmount\":21.00,\"total\":121.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/v1/supplier-invoices/{id} ---

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(supplierInvoiceService.getById(INVOICE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/supplier-invoices/{id}", INVOICE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()))
                .andExpect(jsonPath("$.supplierName").value("Acme Parts"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(supplierInvoiceService.getById(INVOICE_ID))
                .thenThrow(new NotFoundException("SUPPLIER_INVOICE_NOT_FOUND", "Supplier invoice not found"));

        mockMvc.perform(get("/api/v1/supplier-invoices/{id}", INVOICE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_NOT_FOUND"));
    }

    // --- PUT /api/v1/supplier-invoices/{id} ---

    @Test
    void update_returns200_whenValid() throws Exception {
        when(supplierInvoiceService.update(eq(INVOICE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/supplier-invoices/{id}", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\"" + SUPPLIER_ID + "\",\"category\":\"MAINTENANCE\","
                                + "\"invoiceDate\":\"2026-07-01\",\"subtotal\":100.00,\"taxAmount\":21.00,\"total\":121.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_returns409_whenNotPending() throws Exception {
        when(supplierInvoiceService.update(eq(INVOICE_ID), any()))
                .thenThrow(new ConflictException("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION", "Cannot update"));

        mockMvc.perform(put("/api/v1/supplier-invoices/{id}", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":\"" + SUPPLIER_ID + "\",\"category\":\"MAINTENANCE\","
                                + "\"invoiceDate\":\"2026-07-01\",\"subtotal\":100.00,\"taxAmount\":21.00,\"total\":121.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));
    }

    // --- DELETE /api/v1/supplier-invoices/{id} ---

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(supplierInvoiceService).delete(INVOICE_ID);

        mockMvc.perform(delete("/api/v1/supplier-invoices/{id}", INVOICE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenNotPending() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("SUPPLIER_INVOICE_DELETE_NOT_ALLOWED", "Cannot delete"))
                .when(supplierInvoiceService).delete(INVOICE_ID);

        mockMvc.perform(delete("/api/v1/supplier-invoices/{id}", INVOICE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_DELETE_NOT_ALLOWED"));
    }

    // --- PATCH /api/v1/supplier-invoices/{id}/pay ---

    @Test
    void pay_returns200_withoutBody() throws Exception {
        when(supplierInvoiceService.pay(eq(INVOICE_ID), isNull())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/supplier-invoices/{id}/pay", INVOICE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void pay_returns200_withPaymentDateBody() throws Exception {
        when(supplierInvoiceService.pay(eq(INVOICE_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/supplier-invoices/{id}/pay", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\":\"2026-07-01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void pay_returns409_whenNotPending() throws Exception {
        when(supplierInvoiceService.pay(eq(INVOICE_ID), isNull()))
                .thenThrow(new ConflictException("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION", "Cannot pay"));

        mockMvc.perform(patch("/api/v1/supplier-invoices/{id}/pay", INVOICE_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));
    }

    // --- POST /api/v1/supplier-invoices/{id}/line-items ---

    @Test
    void addLineItem_returns201_whenValid() throws Exception {
        SupplierLineItemResponse response = new SupplierLineItemResponse(UUID.randomUUID(), "Parts",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"), null, null);
        when(supplierInvoiceService.addLineItem(eq(INVOICE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/supplier-invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isCreated());
    }

    @Test
    void addLineItem_returns400_whenDescriptionMissing() throws Exception {
        mockMvc.perform(post("/api/v1/supplier-invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void addLineItem_returns404_whenInvoiceMissing() throws Exception {
        when(supplierInvoiceService.addLineItem(eq(INVOICE_ID), any()))
                .thenThrow(new NotFoundException("SUPPLIER_INVOICE_NOT_FOUND", "Supplier invoice not found"));

        mockMvc.perform(post("/api/v1/supplier-invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_NOT_FOUND"));
    }

    @Test
    void addLineItem_returns409_whenInvoiceNotPending() throws Exception {
        when(supplierInvoiceService.addLineItem(eq(INVOICE_ID), any()))
                .thenThrow(new ConflictException("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION", "Cannot add line item"));

        mockMvc.perform(post("/api/v1/supplier-invoices/{id}/line-items", INVOICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Parts\",\"quantity\":2,\"unitPrice\":50.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUPPLIER_INVOICE_INVALID_STATE_TRANSITION"));
    }
}

package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.shared.exception.NotFoundException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock LineItemRepository lineItemRepository;

    PdfExportService pdfExportService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        pdfExportService = new PdfExportService(invoiceRepository, lineItemRepository);
    }

    @Test
    void generateInvoicePdf_returnsNonEmptyPdf_startingWithPdfMagicHeader() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = buildInvoice("INV-2026-00001", "Acme Corp", new BigDecimal("0.2100"));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(invoiceId)).thenReturn(
                List.of(buildLineItem("Oil change", new BigDecimal("2"), new BigDecimal("50.00"))));

        byte[] pdf = pdfExportService.generateInvoicePdf(invoiceId);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoicePdf_containsInvoiceNumberClientAndLineItems() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = buildInvoice("INV-2026-00042", "Acme Corp", new BigDecimal("0.2100"));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(invoiceId)).thenReturn(
                List.of(buildLineItem("Oil change", new BigDecimal("2"), new BigDecimal("50.00"))));

        byte[] pdf = pdfExportService.generateInvoicePdf(invoiceId);
        String text = extractText(pdf);

        assertThat(text).contains("INV-2026-00042");
        assertThat(text).contains("Acme Corp");
        assertThat(text).contains("Oil change");
    }

    @Test
    void generateInvoicePdf_formatsNonDefaultTaxRate_insteadOfHardcoding21Percent() throws Exception {
        // Critical regression guard: taxRate must be read from the invoice, never hardcoded to 21%.
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = buildInvoice("INV-2026-00099", "Acme Corp", new BigDecimal("0.10"));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(lineItemRepository.findAllByInvoiceId(invoiceId)).thenReturn(
                List.of(buildLineItem("Oil change", new BigDecimal("2"), new BigDecimal("50.00"))));

        byte[] pdf = pdfExportService.generateInvoicePdf(invoiceId);
        String text = extractText(pdf);

        assertThat(text).contains("10.00%");
        assertThat(text).doesNotContain("21.00%");
    }

    @Test
    void generateInvoicePdf_throwsNotFound_whenInvoiceMissing() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdfExportService.generateInvoicePdf(invoiceId))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("INVOICE_NOT_FOUND"));
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
            return sb.toString();
        }
    }

    private static Invoice buildInvoice(String invoiceNumber, String clientName, BigDecimal taxRate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        Client client = new Client();
        client.setName(clientName);
        invoice.setClient(client);
        invoice.setTaxRate(taxRate);
        invoice.setIssueDate(LocalDate.of(2026, 7, 1));
        invoice.setDueDate(LocalDate.of(2026, 7, 31));
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxAmount(taxRate.multiply(new BigDecimal("100.00")).setScale(2, java.math.RoundingMode.HALF_UP));
        invoice.setTotal(invoice.getSubtotal().add(invoice.getTaxAmount()));
        return invoice;
    }

    private static InvoiceLineItem buildLineItem(String description, BigDecimal quantity, BigDecimal unitPrice) {
        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setDescription(description);
        lineItem.setQuantity(quantity);
        lineItem.setUnitPrice(unitPrice);
        lineItem.setSubtotal(quantity.multiply(unitPrice));
        return lineItem;
    }
}

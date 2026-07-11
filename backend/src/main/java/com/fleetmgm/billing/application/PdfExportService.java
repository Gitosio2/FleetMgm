package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.shared.exception.NotFoundException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PdfExportService {

    // Same role restriction as the rest of billing — mirrors InvoiceService.ROLES.
    private static final String ROLES = "hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Percentage display scale for the tax rate (e.g. 0.2100 -> "21.00%"). Never hardcode the
    // rate itself — always read invoice.getTaxRate(), which is configurable per-invoice since
    // Hito 31.
    private static final int PERCENTAGE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final InvoiceRepository invoiceRepository;
    private final LineItemRepository lineItemRepository;

    public PdfExportService(InvoiceRepository invoiceRepository, LineItemRepository lineItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize(ROLES)
    public byte[] generateInvoicePdf(UUID invoiceId) {
        Invoice invoice = findInvoiceOrThrow(invoiceId);
        List<InvoiceLineItem> lineItems = lineItemRepository.findAllByInvoiceId(invoiceId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, invoice);
            addLineItemsTable(document, lineItems);
            addTotals(document, invoice);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF for invoice " + invoiceId, e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private Invoice findInvoiceOrThrow(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("INVOICE_NOT_FOUND", "Invoice " + invoiceId + " not found"));
    }

    private void addHeader(Document document, Invoice invoice) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        document.add(new Paragraph("Invoice " + invoice.getInvoiceNumber(), titleFont));
        document.add(new Paragraph("Client: " + invoice.getClient().getName(), normalFont));
        if (invoice.getIssueDate() != null) {
            document.add(new Paragraph("Issue date: " + invoice.getIssueDate().format(DATE_FORMAT), normalFont));
        }
        if (invoice.getDueDate() != null) {
            document.add(new Paragraph("Due date: " + invoice.getDueDate().format(DATE_FORMAT), normalFont));
        }
        document.add(Chunk.NEWLINE);
    }

    private void addLineItemsTable(Document document, List<InvoiceLineItem> lineItems) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell(new PdfPCell(new Phrase("Description", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Quantity", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Unit price", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Subtotal", headerFont)));

        for (InvoiceLineItem item : lineItems) {
            table.addCell(item.getDescription());
            table.addCell(item.getQuantity().toPlainString());
            table.addCell(item.getUnitPrice().toPlainString());
            table.addCell(item.getSubtotal().toPlainString());
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        document.add(new Paragraph("Subtotal: " + invoice.getSubtotal().toPlainString(), normalFont));
        document.add(new Paragraph("Tax rate: " + formatTaxRate(invoice.getTaxRate()), normalFont));
        document.add(new Paragraph("Tax amount: " + invoice.getTaxAmount().toPlainString(), normalFont));
        document.add(new Paragraph("Total: " + invoice.getTotal().toPlainString(), boldFont));
    }

    // Formats invoice.getTaxRate() (a fraction, e.g. 0.2100) as a percentage string (e.g.
    // "21.00%"). The rate itself always comes from the invoice — never a hardcoded constant.
    private String formatTaxRate(BigDecimal taxRate) {
        BigDecimal percentage = taxRate.multiply(ONE_HUNDRED).setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return percentage.toPlainString() + "%";
    }
}

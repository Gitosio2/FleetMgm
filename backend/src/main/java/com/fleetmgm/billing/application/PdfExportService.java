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

    // Mirrors InvoiceService.MONEY_SCALE — same HALF_UP-at-2-decimals convention for currency math.
    private static final int MONEY_SCALE = 2;

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
            addTotals(document, invoice, lineItems);
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

        document.add(new Paragraph("Factura " + invoice.getInvoiceNumber(), titleFont));
        document.add(new Paragraph("Cliente: " + invoice.getClient().getName(), normalFont));
        if (invoice.getIssueDate() != null) {
            document.add(new Paragraph("Fecha de emisión: " + invoice.getIssueDate().format(DATE_FORMAT), normalFont));
        }
        if (invoice.getDueDate() != null) {
            document.add(new Paragraph("Fecha de vencimiento: " + invoice.getDueDate().format(DATE_FORMAT), normalFont));
        }
        document.add(Chunk.NEWLINE);
    }

    private void addLineItemsTable(Document document, List<InvoiceLineItem> lineItems) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell(new PdfPCell(new Phrase("Descripción", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Cantidad", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Precio unitario", headerFont)));
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

    // Computed straight from the line items rather than read off invoice.getSubtotal() /
    // getTaxAmount() / getTotal() — those fields default to BigDecimal.ZERO and are only ever
    // populated by InvoiceService.issue(), so a DRAFT invoice's PDF would otherwise always show
    // 0.00 totals despite having real line items. Line items are frozen once an invoice leaves
    // DRAFT (InvoiceService.assertIsDraft() blocks further add/update), so recomputing here is
    // numerically identical to the stored total for ISSUED/PAID/OVERDUE invoices.
    private void addTotals(Document document, Invoice invoice, List<InvoiceLineItem> lineItems) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        BigDecimal subtotal = lineItems.stream()
                .map(InvoiceLineItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(invoice.getTaxRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        document.add(new Paragraph("Subtotal: " + subtotal.toPlainString(), normalFont));
        document.add(new Paragraph("IVA: " + formatTaxRate(invoice.getTaxRate()), normalFont));
        document.add(new Paragraph("Importe IVA: " + taxAmount.toPlainString(), normalFont));
        document.add(new Paragraph("Total: " + total.toPlainString(), boldFont));
    }

    // Formats invoice.getTaxRate() (a fraction, e.g. 0.2100) as a percentage string (e.g.
    // "21.00%"). The rate itself always comes from the invoice — never a hardcoded constant.
    private String formatTaxRate(BigDecimal taxRate) {
        BigDecimal percentage = taxRate.multiply(ONE_HUNDRED).setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return percentage.toPlainString() + "%";
    }
}

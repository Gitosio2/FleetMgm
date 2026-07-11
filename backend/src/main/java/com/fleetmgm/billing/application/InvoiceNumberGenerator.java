package com.fleetmgm.billing.application;

import com.fleetmgm.billing.infrastructure.InvoiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

// Generates INV-<currentYear>-<5-digit zero-padded sequence> (e.g. INV-2026-00001), pulling the
// next value from the invoice_number_seq PostgreSQL sequence (V16 migration) via a native scalar
// query on InvoiceRepository. A dedicated component (rather than inlining this in InvoiceService)
// keeps the numbering format isolated and independently testable.
@Component
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    public InvoiceNumberGenerator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public String generate() {
        long sequenceValue = invoiceRepository.nextInvoiceNumberSequenceValue();
        return "INV-" + Year.now() + "-" + String.format("%05d", sequenceValue);
    }
}

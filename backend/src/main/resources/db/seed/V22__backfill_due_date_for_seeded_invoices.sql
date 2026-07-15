-- Forward fix for data already seeded by V20: due_date was always NULL for non-draft invoices,
-- even though the column and the app's overdue-detection queries (InvoiceRepository/
-- SupplierInvoiceRepository) rely on it. Written as a new migration instead of editing V20 in
-- place — V20 was already applied wherever this app has been deployed, and editing an
-- already-applied migration's file changes its checksum, which Flyway treats as a validation
-- failure on every subsequent deploy. Net-30 from the issue/invoice date, data-only — no
-- validation added. DRAFT client invoices are left untouched: they have no issue_date either.
UPDATE invoices
SET due_date = issue_date + 30
WHERE due_date IS NULL
  AND status <> 'DRAFT';

-- SupplierInvoiceStatus has no DRAFT concept — every row is either PENDING or PAID, so every row
-- should carry a due date.
UPDATE supplier_invoices
SET due_date = invoice_date + 30
WHERE due_date IS NULL;

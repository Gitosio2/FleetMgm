-- MaintenanceRecord.invoice was fully dead: setInvoice(...) was never called anywhere in
-- production code. InvoiceLineItem.linkedMaintenance was reachable but doesn't represent a
-- real business scenario for this app (a client invoice never directly bills a maintenance
-- job — that relationship correctly belongs to SupplierInvoiceLineItem.maintenanceRecord,
-- which stays untouched).
ALTER TABLE maintenance_records DROP CONSTRAINT fk_maintenance_invoice;
ALTER TABLE maintenance_records DROP COLUMN invoice_id;
ALTER TABLE invoice_line_items DROP COLUMN linked_maintenance_id;

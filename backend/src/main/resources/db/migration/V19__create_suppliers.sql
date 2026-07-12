CREATE TABLE suppliers (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    tax_id      VARCHAR(20),
    email       VARCHAR(255),
    phone       VARCHAR(30),
    address     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

-- Partial unique index: taxId is nullable (backfilled suppliers won't have one yet) but must be
-- unique when present. Same pattern as uq_vehicles_license_plate_active (V9).
CREATE UNIQUE INDEX uq_suppliers_tax_id_active
    ON suppliers (tax_id)
    WHERE tax_id IS NOT NULL AND deleted_at IS NULL;

-- Backfill: one Supplier per distinct supplier_name already used in supplier_invoices, so existing
-- free-text data is not lost when supplier_invoices.supplier_name is replaced by a mandatory FK.
INSERT INTO suppliers (name)
SELECT DISTINCT supplier_name FROM supplier_invoices;

ALTER TABLE supplier_invoices ADD COLUMN supplier_id UUID REFERENCES suppliers (id);

UPDATE supplier_invoices si
SET supplier_id = s.id
FROM suppliers s
WHERE s.name = si.supplier_name;

ALTER TABLE supplier_invoices ALTER COLUMN supplier_id SET NOT NULL;
ALTER TABLE supplier_invoices DROP COLUMN supplier_name;

CREATE INDEX idx_supplier_invoices_supplier_id ON supplier_invoices (supplier_id);

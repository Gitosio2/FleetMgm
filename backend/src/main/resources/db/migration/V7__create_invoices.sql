CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number  VARCHAR(20)  NOT NULL,
    client_id       UUID         NOT NULL REFERENCES clients (id),
    status          VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
    issue_date      DATE,
    due_date        DATE,
    payment_date    DATE,
    tax_rate        NUMERIC(5, 4) NOT NULL DEFAULT 0.2100,
    subtotal        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_amount      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total           NUMERIC(12, 2) NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_invoices_number UNIQUE (invoice_number)
);

CREATE INDEX idx_invoices_client_id ON invoices (client_id);

CREATE TABLE invoice_line_items (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id           UUID           NOT NULL REFERENCES invoices (id),
    description          VARCHAR(500)   NOT NULL,
    quantity             NUMERIC(10, 2) NOT NULL,
    unit_price           NUMERIC(10, 2) NOT NULL,
    subtotal             NUMERIC(12, 2) NOT NULL,
    linked_job_id        UUID           REFERENCES jobs (id),
    linked_maintenance_id UUID          REFERENCES maintenance_records (id)
);

-- Wire up the deferred FK from maintenance_records → invoices
ALTER TABLE maintenance_records
    ADD CONSTRAINT fk_maintenance_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id);

CREATE TABLE supplier_invoices (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_name           VARCHAR(255)   NOT NULL,
    supplier_invoice_number VARCHAR(100),
    category                VARCHAR(20)    NOT NULL,
    invoice_date            DATE           NOT NULL,
    due_date                DATE,
    payment_date            DATE,
    status                  VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    subtotal                NUMERIC(12, 2) NOT NULL,
    tax_amount              NUMERIC(12, 2) NOT NULL,
    total                   NUMERIC(12, 2) NOT NULL,
    vehicle_id              UUID           REFERENCES vehicles (id),
    notes                   TEXT,
    document_path           VARCHAR(500),
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_supplier_invoices_vehicle_id ON supplier_invoices (vehicle_id);

CREATE TABLE supplier_invoice_line_items (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id            UUID           NOT NULL REFERENCES supplier_invoices (id),
    description           VARCHAR(500)   NOT NULL,
    quantity              NUMERIC(10, 2) NOT NULL,
    unit_price            NUMERIC(10, 2) NOT NULL,
    subtotal              NUMERIC(12, 2) NOT NULL,
    vehicle_id            UUID           REFERENCES vehicles (id),
    maintenance_record_id UUID           REFERENCES maintenance_records (id)
);

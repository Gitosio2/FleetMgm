-- maintenance_records references invoices which don't exist yet;
-- the invoice FK is added in V7 after invoices table is created.
CREATE TABLE maintenance_records (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id           UUID        NOT NULL REFERENCES vehicles (id),
    type                 VARCHAR(100) NOT NULL,
    description          TEXT,
    usage_at_service     BIGINT,
    cost                 NUMERIC(10, 2),
    workshop_entry_date  DATE,
    workshop_exit_date   DATE,
    technician_id        UUID        REFERENCES workers (id),
    invoice_id           UUID,       -- FK added in V7__create_invoices.sql
    status               VARCHAR(15) NOT NULL DEFAULT 'SCHEDULED',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_maintenance_vehicle_id ON maintenance_records (vehicle_id);

CREATE TABLE workshop_schedules (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id            UUID        NOT NULL REFERENCES vehicles (id),
    technician_id         UUID        REFERENCES workers (id),
    maintenance_record_id UUID        REFERENCES maintenance_records (id),
    scheduled_date        DATE        NOT NULL,
    type                  VARCHAR(100) NOT NULL,
    priority              VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status                VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    notes                 TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_workshop_schedules_vehicle_id ON workshop_schedules (vehicle_id);
CREATE INDEX idx_workshop_schedules_date       ON workshop_schedules (scheduled_date);

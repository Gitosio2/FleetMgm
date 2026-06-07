CREATE TABLE gps_positions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id  UUID        NOT NULL REFERENCES vehicles (id),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    heading     DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL,
    source      VARCHAR(10) NOT NULL
);

CREATE INDEX idx_gps_vehicle_id  ON gps_positions (vehicle_id);
CREATE INDEX idx_gps_recorded_at ON gps_positions (recorded_at DESC);

CREATE TABLE audit_logs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type           VARCHAR(100) NOT NULL,
    entity_id             VARCHAR(36)  NOT NULL,
    action                VARCHAR(20)  NOT NULL,
    performed_by_user_id  UUID,
    performed_by_email    VARCHAR(255),
    performed_at          TIMESTAMPTZ  NOT NULL,
    ip_address            VARCHAR(45),
    old_values            JSONB,
    new_values            JSONB,
    details               TEXT
);

CREATE INDEX idx_audit_performed_at  ON audit_logs (performed_at DESC);
CREATE INDEX idx_audit_entity        ON audit_logs (entity_type, entity_id);

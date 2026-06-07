CREATE TABLE jobs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    vehicle_id            UUID        NOT NULL REFERENCES vehicles (id),
    assigned_driver_id    UUID        REFERENCES workers (id),
    client_id             UUID        REFERENCES clients (id),
    status                VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    origin_location       VARCHAR(500) NOT NULL,
    destination_location  VARCHAR(500) NOT NULL,
    notes                 TEXT,
    scheduled_start       TIMESTAMPTZ,
    scheduled_end         TIMESTAMPTZ,
    actual_start          TIMESTAMPTZ,
    actual_end            TIMESTAMPTZ,
    start_usage_value     BIGINT,
    end_usage_value       BIGINT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ
);

CREATE INDEX idx_jobs_vehicle_id        ON jobs (vehicle_id);
CREATE INDEX idx_jobs_assigned_driver   ON jobs (assigned_driver_id);
CREATE INDEX idx_jobs_status            ON jobs (status) WHERE deleted_at IS NULL;

CREATE TABLE usage_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id   UUID        NOT NULL REFERENCES vehicles (id),
    value        BIGINT      NOT NULL,
    measure_type VARCHAR(10) NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL,
    source       VARCHAR(20) NOT NULL,
    job_id       UUID        REFERENCES jobs (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usage_logs_vehicle_id ON usage_logs (vehicle_id);

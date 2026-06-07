CREATE TABLE workers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users (id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    worker_role     VARCHAR(15)  NOT NULL,
    phone           VARCHAR(30),
    national_id     VARCHAR(20)  NOT NULL,
    license_type    VARCHAR(20),
    license_expiry  DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_workers_user_id UNIQUE (user_id)
);

CREATE TABLE driver_vehicle_assignments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id            UUID        NOT NULL REFERENCES workers (id),
    vehicle_id           UUID        NOT NULL REFERENCES vehicles (id),
    start_date           DATE        NOT NULL,
    end_date             DATE,
    assigned_by_user_id  UUID        NOT NULL REFERENCES users (id),
    notes                VARCHAR(1000),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Guarantees a driver has at most one active assignment (end_date IS NULL) at a time
CREATE UNIQUE INDEX uq_active_assignment_per_driver
    ON driver_vehicle_assignments (driver_id)
    WHERE end_date IS NULL;

CREATE INDEX idx_assignments_vehicle_id ON driver_vehicle_assignments (vehicle_id);

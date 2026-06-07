CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    tax_id      VARCHAR(20)  NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(30),
    address     VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT uq_clients_tax_id UNIQUE (tax_id)
);

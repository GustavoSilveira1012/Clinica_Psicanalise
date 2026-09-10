CREATE TABLE service_invoices (

    id UUID PRIMARY KEY,

    fiscal_issuer_id UUID NOT NULL,

    fiscal_configuration_id UUID NOT NULL,

    patient_id BIGINT,

    status VARCHAR(40) NOT NULL,

    provider VARCHAR(30) NOT NULL,

    environment VARCHAR(20) NOT NULL,

    competence_date DATE NOT NULL,

    service_amount NUMERIC(19,2) NOT NULL,

    unconditional_discount NUMERIC(19,2)
        NOT NULL DEFAULT 0,

    conditional_discount NUMERIC(19,2)
        NOT NULL DEFAULT 0,

    deduction_amount NUMERIC(19,2)
        NOT NULL DEFAULT 0,

    taxable_amount NUMERIC(19,2) NOT NULL,

    net_amount NUMERIC(19,2) NOT NULL,

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    national_service_code VARCHAR(50),

    municipal_service_code VARCHAR(50),

    nbs_code VARCHAR(50),

    dps_series VARCHAR(5),

    dps_number BIGINT,

    nfse_number VARCHAR(100),

    access_key VARCHAR(255),

    provider_reference VARCHAR(255),

    replaces_invoice_id UUID,

    replaced_by_invoice_id UUID,

    issued_at TIMESTAMPTZ,

    authorized_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,

    rejected_at TIMESTAMPTZ,

    rejection_code VARCHAR(100),

    rejection_message VARCHAR(1000),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_invoice_issuer
        FOREIGN KEY (fiscal_issuer_id)
        REFERENCES fiscal_issuers(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invoice_configuration
        FOREIGN KEY (fiscal_configuration_id)
        REFERENCES fiscal_configurations(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invoice_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invoice_replaces
        FOREIGN KEY (replaces_invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invoice_replaced_by
        FOREIGN KEY (replaced_by_invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_service_invoice_amount
        CHECK (
            service_amount > 0
            AND taxable_amount >= 0
            AND net_amount >= 0
        ),

    CONSTRAINT chk_service_invoice_status
        CHECK (
            status IN (
                'DRAFT',
                'PENDING',
                'PROCESSING',
                'AUTHORIZED',
                'REJECTED',
                'CANCEL_PENDING',
                'CANCELLED',
                'ERROR',
                'RECONCILIATION_REQUIRED'
            )
        )
);

--------------------------------------------------

CREATE UNIQUE INDEX ux_service_invoice_dps

ON service_invoices(
    fiscal_issuer_id,
    dps_series,
    dps_number
)

WHERE dps_number IS NOT NULL;

CREATE UNIQUE INDEX ux_service_invoice_access_key

ON service_invoices(access_key)

WHERE access_key IS NOT NULL;

---------------------------------------------------------

CREATE TABLE service_invoice_party_snapshots (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL,

    party_type VARCHAR(20) NOT NULL,

    encrypted_snapshot BYTEA NOT NULL,

    snapshot_iv BYTEA NOT NULL,

    encrypted_snapshot_dek BYTEA NOT NULL,

    key_id VARCHAR(255) NOT NULL,

    crypto_version INTEGER NOT NULL,

    crypto_algorithm VARCHAR(30) NOT NULL,

    snapshot_sha256 VARCHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_party_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_fiscal_party_invoice_type
        UNIQUE (
            invoice_id,
            party_type
        ),

    CONSTRAINT chk_fiscal_party_type
        CHECK (
            party_type IN (
                'ISSUER',
                'TAKER'
            )
        )
);

---------------------------------------------------

ALTER TABLE service_invoices
ADD COLUMN encrypted_service_description BYTEA;

ALTER TABLE service_invoices
ADD COLUMN service_description_iv BYTEA;

ALTER TABLE service_invoices
ADD COLUMN encrypted_service_description_dek BYTEA;

ALTER TABLE service_invoices
ADD COLUMN service_description_key_id VARCHAR(255);

ALTER TABLE service_invoices
ADD COLUMN service_description_crypto_version INTEGER;

ALTER TABLE service_invoices
ADD COLUMN service_description_crypto_algorithm VARCHAR(30);

---------------------------------------------

CREATE TABLE service_invoice_origins (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL,

    receivable_id UUID NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_invoice_origin_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_invoice_origin_receivable
        FOREIGN KEY (receivable_id)
        REFERENCES receivables(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_invoice_origin_amount
        CHECK (amount > 0),

    CONSTRAINT ux_invoice_origin_receivable
        UNIQUE (
            invoice_id,
            receivable_id
        )
);
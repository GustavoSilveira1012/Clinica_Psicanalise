CREATE TABLE service_invoice_tax_snapshots (

    id UUID PRIMARY KEY,

    invoice_id UUID NOT NULL UNIQUE,

    schema_version VARCHAR(50) NOT NULL,

    source VARCHAR(50) NOT NULL,

    taxable_base NUMERIC(19,2) NOT NULL,

    iss_rate NUMERIC(12,6),

    iss_amount NUMERIC(19,2),

    iss_withheld BOOLEAN,

    ibs_amount NUMERIC(19,2),

    cbs_amount NUMERIC(19,2),

    pis_amount NUMERIC(19,2),

    cofins_amount NUMERIC(19,2),

    inss_amount NUMERIC(19,2),

    ir_amount NUMERIC(19,2),

    csll_amount NUMERIC(19,2),

    total_tax_amount NUMERIC(19,2),

    municipal_parameters_hash VARCHAR(64),

    parameters_fetched_at TIMESTAMPTZ,

    details JSONB NOT NULL
        DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tax_snapshot_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE RESTRICT
);

------------------------------------------------------------

CREATE TABLE dps_sequences (

    fiscal_issuer_id UUID NOT NULL,

    series VARCHAR(5) NOT NULL,

    next_number BIGINT NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (
        fiscal_issuer_id,
        series
    ),

    CONSTRAINT fk_dps_sequence_issuer
        FOREIGN KEY (fiscal_issuer_id)
        REFERENCES fiscal_issuers(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_dps_next_number
        CHECK (next_number > 0)
);
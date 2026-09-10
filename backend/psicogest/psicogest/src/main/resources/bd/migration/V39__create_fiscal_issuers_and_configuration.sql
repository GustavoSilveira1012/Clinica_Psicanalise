CREATE TABLE fiscal_issuers (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    tax_identifier_type VARCHAR(10) NOT NULL,

    tax_identifier_hash VARCHAR(64) NOT NULL,

    tax_identifier_last4 VARCHAR(4),

    encrypted_tax_identifier BYTEA NOT NULL,

    tax_identifier_iv BYTEA NOT NULL,

    encrypted_tax_identifier_dek BYTEA NOT NULL,

    tax_identifier_key_id VARCHAR(255) NOT NULL,

    tax_identifier_crypto_version INTEGER NOT NULL,

    tax_identifier_crypto_algorithm VARCHAR(30) NOT NULL,

    municipal_registration VARCHAR(50),

    municipality_code VARCHAR(7) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_issuer_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_fiscal_issuer_identifier_type
        CHECK (
            tax_identifier_type IN (
                'CPF',
                'CNPJ'
            )
        )
);


CREATE UNIQUE INDEX ux_fiscal_issuer_tax_identifier

ON fiscal_issuers(
    financial_entity_id,
    tax_identifier_hash,
    municipality_code
);

---------------------------------------------------------------

CREATE TABLE fiscal_configurations (

    id UUID PRIMARY KEY,

    fiscal_issuer_id UUID NOT NULL,

    version INTEGER NOT NULL,

    provider VARCHAR(30) NOT NULL,

    environment VARCHAR(20) NOT NULL,

    tax_regime VARCHAR(30) NOT NULL,

    municipality_code VARCHAR(7) NOT NULL,

    national_service_code VARCHAR(50),

    municipal_service_code VARCHAR(50),

    nbs_code VARCHAR(50),

    special_tax_regime_code VARCHAR(50),

    layout_version VARCHAR(50) NOT NULL,

    credential_ref VARCHAR(500),

    provider_configuration JSONB NOT NULL
        DEFAULT '{}'::jsonb,

    effective_from DATE NOT NULL,

    effective_to DATE,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_fiscal_configuration_issuer
        FOREIGN KEY (fiscal_issuer_id)
        REFERENCES fiscal_issuers(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_fiscal_configuration_version
        UNIQUE (
            fiscal_issuer_id,
            version
        ),

    CONSTRAINT chk_fiscal_environment
        CHECK (
            environment IN (
                'HOMOLOGATION',
                'PRODUCTION'
            )
        )
);

---------------------------------------------

ALTER TABLE fiscal_configurations

ADD CONSTRAINT ex_fiscal_configuration_period

EXCLUDE USING gist (

    fiscal_issuer_id WITH =,

    daterange(
        effective_from,
        COALESCE(
            effective_to,
            'infinity'::date
        ),
        '[)'
    ) WITH &&
);
-- V40: Criar tabelas de emissores e configurações fiscais

CREATE TABLE fiscal_issuers (
    id UUID PRIMARY KEY,
    clinic_id BIGINT NOT NULL,
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    default_tax_regime VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT,
    
    CONSTRAINT fk_fiscal_issuer_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_fiscal_issuer_clinic ON fiscal_issuers (clinic_id);
CREATE INDEX idx_fiscal_issuer_active ON fiscal_issuers (active);

COMMENT ON TABLE fiscal_issuers IS 'Emissores fiscais (clínica como contribuinte)';

-- Configurações fiscais por período
CREATE TABLE fiscal_configurations (
    id UUID PRIMARY KEY,
    issuer_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    layout_version VARCHAR(10),
    validity_start DATE NOT NULL,
    validity_end DATE,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT,
    
    CONSTRAINT fk_fiscal_config_issuer
        FOREIGN KEY (issuer_id)
        REFERENCES fiscal_issuers(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_fiscal_config_provider
        FOREIGN KEY (provider_id)
        REFERENCES fiscal_providers(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_fiscal_config_issuer ON fiscal_configurations (issuer_id);
CREATE INDEX idx_fiscal_config_period ON fiscal_configurations (validity_start, validity_end);
CREATE INDEX idx_fiscal_config_effective ON fiscal_configurations (issuer_id, validity_start, validity_end);

COMMENT ON TABLE fiscal_configurations IS 'Configurações fiscais vigentes em períodos específicos';

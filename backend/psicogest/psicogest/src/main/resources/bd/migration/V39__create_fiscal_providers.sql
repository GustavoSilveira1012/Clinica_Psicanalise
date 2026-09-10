-- V39: Criar tabela de provedores fiscais

CREATE TABLE fiscal_providers (
    id UUID PRIMARY KEY,
    clinic_id BIGINT NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    tax_regime VARCHAR(50) NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    encrypted_credentials TEXT NOT NULL,
    credentials_iv VARCHAR(24) NOT NULL,
    credentials_key_id VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT,
    
    CONSTRAINT fk_fiscal_provider_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_fiscal_provider_clinic ON fiscal_providers (clinic_id);
CREATE INDEX idx_fiscal_provider_type ON fiscal_providers (provider_type);
CREATE INDEX idx_fiscal_provider_active ON fiscal_providers (active);

COMMENT ON TABLE fiscal_providers IS 'Provedores fiscais configurados para cada clínica';
COMMENT ON COLUMN fiscal_providers.encrypted_credentials IS 'Credenciais encriptadas com AES-256-GCM (cnpj, username, password, token)';

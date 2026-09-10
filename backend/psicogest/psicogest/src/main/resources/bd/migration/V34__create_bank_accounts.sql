-- V34__create_bank_accounts.sql
-- Criar tabela de contas bancárias

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY NOT NULL,
    clinic_id UUID NOT NULL REFERENCES clinics(id),
    account_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(3) NOT NULL,
    branch VARCHAR(10) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_holder VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_bank_account_clinic ON bank_accounts(clinic_id);
CREATE INDEX idx_bank_account_active ON bank_accounts(active);

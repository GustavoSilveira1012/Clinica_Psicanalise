-- V35__create_bank_transactions.sql
-- Criar tabela de transações bancárias com criptografia de description

CREATE TABLE bank_transactions (
    id UUID PRIMARY KEY NOT NULL,
    bank_account_id UUID NOT NULL REFERENCES bank_accounts(id),
    external_transaction_id VARCHAR(255),
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('CREDIT', 'DEBIT')),
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    booking_date DATE NOT NULL,
    posted_at TIMESTAMP,
    
    -- Description criptografado (envelope encryption)
    encrypted_description BYTEA,
    description_iv BYTEA,
    description_encrypted_dek BYTEA,
    crypto_version INTEGER,
    crypto_algorithm VARCHAR(30),
    key_id VARCHAR(255),
    
    reference VARCHAR(255),
    transaction_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNRECONCILED' CHECK (status IN ('UNRECONCILED', 'PARTIALLY_RECONCILED', 'RECONCILED', 'DISPUTED')),
    
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Índices
CREATE INDEX idx_bank_account_booking_date ON bank_transactions(bank_account_id, booking_date);
CREATE INDEX idx_bank_account_status ON bank_transactions(bank_account_id, status);
CREATE INDEX idx_bank_transaction_posted_at ON bank_transactions(posted_at);

-- UNIQUE constraints
CREATE UNIQUE INDEX ux_bank_transaction_fingerprint 
    ON bank_transactions(bank_account_id, transaction_fingerprint);

-- UNIQUE constraint para external_id (onde não é null)
-- PostgreSQL permite partial indexes
CREATE UNIQUE INDEX ux_bank_transaction_external_id 
    ON bank_transactions(bank_account_id, external_transaction_id) 
    WHERE external_transaction_id IS NOT NULL;

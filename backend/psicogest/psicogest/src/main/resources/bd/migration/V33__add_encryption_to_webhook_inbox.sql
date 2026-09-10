-- 13. Adicionar criptografia de envelope ao webhook inbox
-- 
-- Payload do webhook criptografado com AES-256-GCM
-- Chave dedicada WEBHOOK_DATA_KEK
-- Nunca reutilizar CLINICAL_DATA_KEK, MFA key ou Audit key

ALTER TABLE payment_webhook_inbox
ADD COLUMN encrypted_payload BYTEA NOT NULL DEFAULT ''::bytea;

ALTER TABLE payment_webhook_inbox
ADD COLUMN payload_iv BYTEA NOT NULL DEFAULT ''::bytea;

ALTER TABLE payment_webhook_inbox
ADD COLUMN encrypted_dek BYTEA NOT NULL DEFAULT ''::bytea;

ALTER TABLE payment_webhook_inbox
ADD COLUMN crypto_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE payment_webhook_inbox
ADD COLUMN crypto_algorithm VARCHAR(30) NOT NULL DEFAULT 'AES-256-GCM';

ALTER TABLE payment_webhook_inbox
ADD COLUMN key_id VARCHAR(255) NOT NULL DEFAULT 'WEBHOOK_DATA_KEK';

-- Remover valores default após população
ALTER TABLE payment_webhook_inbox
ALTER COLUMN encrypted_payload DROP DEFAULT;

ALTER TABLE payment_webhook_inbox
ALTER COLUMN payload_iv DROP DEFAULT;

ALTER TABLE payment_webhook_inbox
ALTER COLUMN encrypted_dek DROP DEFAULT;

ALTER TABLE payment_webhook_inbox
ALTER COLUMN crypto_version DROP DEFAULT;

ALTER TABLE payment_webhook_inbox
ALTER COLUMN crypto_algorithm DROP DEFAULT;

ALTER TABLE payment_webhook_inbox
ALTER COLUMN key_id DROP DEFAULT;

-- Índices para performance
CREATE INDEX idx_webhook_provider_status
ON payment_webhook_inbox(
    provider,
    status
);

CREATE INDEX idx_webhook_key_id
ON payment_webhook_inbox(key_id);

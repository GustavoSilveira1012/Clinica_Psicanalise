-- v1.0: envelope metadata for encrypted invite email recovery. The raw email
-- is never persisted; email_hash remains the lookup/deduplication key.

ALTER TABLE organization_invites ADD COLUMN email_encrypted_dek BYTEA;
ALTER TABLE organization_invites ADD COLUMN email_key_id VARCHAR(255);
ALTER TABLE organization_invites ADD COLUMN email_crypto_version INTEGER;
ALTER TABLE organization_invites ADD COLUMN email_crypto_algorithm VARCHAR(30);

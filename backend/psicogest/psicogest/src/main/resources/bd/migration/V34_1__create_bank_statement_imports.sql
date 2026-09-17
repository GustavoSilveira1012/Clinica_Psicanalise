-- Statement import metadata is kept separate from the canonical bank account
-- table created in V34.
CREATE TABLE bank_statement_imports (
    id UUID PRIMARY KEY,
    bank_account_id UUID NOT NULL,
    source VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    source_sha256 VARCHAR(64) NOT NULL,
    transaction_count INTEGER NOT NULL DEFAULT 0,
    imported_by_user_id BIGINT,
    imported_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_statement_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_statement_import_user FOREIGN KEY (imported_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ux_statement_source_hash UNIQUE (bank_account_id, source_sha256),
    CONSTRAINT chk_statement_source CHECK (source IN ('OFX', 'BANK_API')),
    CONSTRAINT chk_statement_status CHECK (status IN ('PROCESSING', 'IMPORTED', 'FAILED'))
);

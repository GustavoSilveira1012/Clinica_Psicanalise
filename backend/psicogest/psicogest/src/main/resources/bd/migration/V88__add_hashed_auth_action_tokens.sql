CREATE TABLE auth_action_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(32) NOT NULL CHECK (token_type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    security_version INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_auth_action_token_expiration CHECK (expires_at > created_at),
    CONSTRAINT chk_auth_action_token_consumption CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);

CREATE INDEX idx_auth_action_tokens_user_pending
    ON auth_action_tokens(user_id, token_type, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE auth_action_tokens IS
    'Stores only hashes of one-time email-verification and password-reset tokens; raw tokens are never persisted.';

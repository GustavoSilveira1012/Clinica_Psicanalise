CREATE TABLE refresh_tokens (

    id UUID PRIMARY KEY,

    user_id BIGINT NOT NULL,

    family_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    security_version INTEGER NOT NULL,

    issued_at TIMESTAMP NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    consumed_at TIMESTAMP,

    revoked_at TIMESTAMP,

    revocation_reason VARCHAR(100),

    replaced_by_id UUID,

    created_ip VARCHAR(45),

    user_agent_hash VARCHAR(64),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_refresh_token_replacement
        FOREIGN KEY (replaced_by_id)
        REFERENCES refresh_tokens(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_refresh_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_refresh_expiration
        CHECK (
            expires_at > issued_at
        )
);


CREATE INDEX idx_refresh_token_user
    ON refresh_tokens(user_id);


CREATE INDEX idx_refresh_token_family
    ON refresh_tokens(family_id);


CREATE INDEX idx_refresh_token_expires
    ON refresh_tokens(expires_at);
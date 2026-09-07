CREATE TABLE IF NOT EXISTS authentication_challenges (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    challenge_type VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    security_version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_ip VARCHAR(45),
    user_agent_hash VARCHAR(64),

    CONSTRAINT fk_auth_challenge_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_auth_challenge_type
        CHECK (
            challenge_type IN (
                'MFA_REQUIRED',
                'MFA_ENROLLMENT_REQUIRED'
            )
        ),

    CONSTRAINT chk_auth_challenge_attempts
        CHECK (
            attempt_count >= 0
        )
);


CREATE INDEX idx_auth_challenge_user
    ON authentication_challenges(user_id);

CREATE INDEX idx_auth_challenge_expiration
    ON authentication_challenges(expires_at);

CREATE TABLE mfa_methods (

    id UUID PRIMARY KEY,

    user_id BIGINT NOT NULL,

    method_type VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    label VARCHAR(100),

    secret_ciphertext TEXT,

    secret_iv VARCHAR(100),

    last_accepted_time_step BIGINT,
    enrollment_challenge_id UUID REFERENCES authentication_challenges(id) ON DELETE RESTRICT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    verified_at TIMESTAMP,

    last_used_at TIMESTAMP,

    revoked_at TIMESTAMP,

    CONSTRAINT fk_mfa_method_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_mfa_method_type
        CHECK (
            method_type IN (
                'TOTP'
            )
        ),

    CONSTRAINT chk_mfa_method_status
        CHECK (
            status IN (
                'PENDING',
                'ACTIVE',
                'REVOKED'
            )
        )
);

CREATE UNIQUE INDEX ux_user_active_totp
    ON mfa_methods(user_id)
    WHERE method_type = 'TOTP'
      AND status = 'ACTIVE';

CREATE UNIQUE INDEX ux_user_pending_totp
    ON mfa_methods(user_id)
    WHERE method_type = 'TOTP' AND status = 'PENDING';

CREATE TABLE mfa_recovery_codes (

    id UUID PRIMARY KEY,

    user_id BIGINT NOT NULL,

    code_hash VARCHAR(64) NOT NULL UNIQUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    used_at TIMESTAMP,

    CONSTRAINT fk_recovery_code_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_recovery_code_user
    ON mfa_recovery_codes(user_id);

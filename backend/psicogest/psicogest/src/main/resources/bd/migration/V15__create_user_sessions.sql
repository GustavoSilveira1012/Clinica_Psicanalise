CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_ip VARCHAR(45),
    last_ip VARCHAR(45),
    user_agent_hash VARCHAR(64),
    device_label VARCHAR(100),
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(100)
);
CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_active ON user_sessions(user_id, revoked_at);

-- Preserve existing refresh families, including families already revoked.
INSERT INTO user_sessions (
    id, user_id, created_at, last_seen_at, expires_at, created_ip, last_ip,
    revoked_at, revocation_reason
)
SELECT family_id, user_id, MIN(issued_at), MAX(issued_at), MAX(expires_at),
       MIN(created_ip), MIN(created_ip),
       CASE WHEN BOOL_AND(revoked_at IS NOT NULL) THEN MAX(revoked_at) END,
       CASE WHEN BOOL_AND(revoked_at IS NOT NULL) THEN 'MIGRATED_REVOKED_FAMILY' END
FROM refresh_tokens
GROUP BY family_id, user_id
ON CONFLICT (id) DO NOTHING;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_session
    FOREIGN KEY (family_id) REFERENCES user_sessions(id) ON DELETE RESTRICT;

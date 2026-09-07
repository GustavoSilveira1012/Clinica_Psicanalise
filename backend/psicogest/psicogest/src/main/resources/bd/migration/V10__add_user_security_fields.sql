ALTER TABLE users

    ADD COLUMN security_version INTEGER
        NOT NULL DEFAULT 1,

    ADD COLUMN failed_login_attempts INTEGER
        NOT NULL DEFAULT 0,

    ADD COLUMN last_failed_login_at TIMESTAMP,

    ADD COLUMN locked_until TIMESTAMP,

    ADD COLUMN last_login_at TIMESTAMP,

    ADD COLUMN password_changed_at TIMESTAMP,

    ADD COLUMN require_password_change BOOLEAN
        NOT NULL DEFAULT FALSE;


ALTER TABLE users
    ADD CONSTRAINT chk_user_security_version
        CHECK (security_version >= 1);


ALTER TABLE users
    ADD CONSTRAINT chk_failed_login_attempts
        CHECK (failed_login_attempts >= 0);
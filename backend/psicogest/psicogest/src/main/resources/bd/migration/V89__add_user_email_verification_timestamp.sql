ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMP;

COMMENT ON COLUMN users.email_verified_at IS
    'Timestamp when the account email was verified with a one-time action token; NULL means not verified.';

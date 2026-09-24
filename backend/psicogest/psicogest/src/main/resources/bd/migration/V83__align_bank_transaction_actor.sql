-- User IDs are BIGINT. Preserve the old UUID column for forensic attribution;
-- there is no trustworthy automatic UUID-to-user mapping.
ALTER TABLE bank_transactions ADD COLUMN ignored_by_user_id BIGINT
    REFERENCES users(id) ON DELETE RESTRICT;
COMMENT ON COLUMN bank_transactions.ignored_by IS
    'Legacy UUID actor reference. Retained verbatim; requires explicit reconciliation if populated.';

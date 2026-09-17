-- A pending invite can be looked up by its one-time secret without exposing
-- the tenant. The application sets only the SHA-256 hash in this transaction.

CREATE POLICY organization_invites_token_lookup ON organization_invites
    USING (token_hash = current_setting('app.invite_token_hash', true));

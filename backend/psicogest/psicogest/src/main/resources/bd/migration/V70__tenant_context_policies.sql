-- v1.0: allow the authenticated user to select one of their organizations.
-- app.user_id is set from the verified JWT, never from a request header.

CREATE OR REPLACE FUNCTION app.current_user_id()
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    raw_value TEXT;
BEGIN
    raw_value := current_setting('app.user_id', true);
    IF raw_value IS NULL OR raw_value = '' THEN
        RETURN NULL;
    END IF;
    RETURN raw_value::BIGINT;
EXCEPTION WHEN invalid_text_representation THEN
    RETURN NULL;
END;
$$;

DROP POLICY organizations_tenant_isolation ON organizations;
CREATE POLICY organizations_tenant_isolation ON organizations
    USING (id = app.current_organization_id() OR owner_user_id = app.current_user_id())
    WITH CHECK (owner_user_id = app.current_user_id());

DROP POLICY organization_memberships_tenant_isolation ON organization_memberships;
CREATE POLICY organization_memberships_tenant_isolation ON organization_memberships
    USING (organization_id = app.current_organization_id() OR user_id = app.current_user_id())
    WITH CHECK (organization_id = app.current_organization_id());

CREATE INDEX idx_audit_logs_organization ON audit_logs(organization_id, occurred_at DESC);
CREATE INDEX idx_security_events_organization ON security_events(organization_id, occurred_at DESC);

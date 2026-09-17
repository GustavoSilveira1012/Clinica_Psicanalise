-- v1.0 RLS phase for newly-created SaaS tables. Legacy table RLS is enabled
-- only after all legacy services set the transaction-local tenant context.

ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE organizations FORCE ROW LEVEL SECURITY;
CREATE POLICY organizations_tenant_isolation ON organizations
    USING (id = app.current_organization_id())
    WITH CHECK (id = app.current_organization_id());

ALTER TABLE organization_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_memberships FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_memberships_tenant_isolation ON organization_memberships
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE organization_invites ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_invites FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_invites_tenant_isolation ON organization_invites
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE saas_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE saas_subscriptions FORCE ROW LEVEL SECURITY;
CREATE POLICY saas_subscriptions_tenant_isolation ON saas_subscriptions
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE saas_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE saas_invoices FORCE ROW LEVEL SECURITY;
CREATE POLICY saas_invoices_tenant_isolation ON saas_invoices
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE saas_usage_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE saas_usage_events FORCE ROW LEVEL SECURITY;
CREATE POLICY saas_usage_events_tenant_isolation ON saas_usage_events
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE saas_usage_monthly ENABLE ROW LEVEL SECURITY;
ALTER TABLE saas_usage_monthly FORCE ROW LEVEL SECURITY;
CREATE POLICY saas_usage_monthly_tenant_isolation ON saas_usage_monthly
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE organization_onboarding ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_onboarding FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_onboarding_tenant_isolation ON organization_onboarding
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE support_access_grants ENABLE ROW LEVEL SECURITY;
ALTER TABLE support_access_grants FORCE ROW LEVEL SECURITY;
CREATE POLICY support_access_grants_tenant_isolation ON support_access_grants
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE organization_agreement_acceptances ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_agreement_acceptances FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_agreement_tenant_isolation ON organization_agreement_acceptances
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

ALTER TABLE feature_flags ENABLE ROW LEVEL SECURITY;
ALTER TABLE feature_flags FORCE ROW LEVEL SECURITY;
CREATE POLICY feature_flags_tenant_isolation ON feature_flags
    USING (organization_id IS NULL OR organization_id = app.current_organization_id())
    WITH CHECK (organization_id IS NULL OR organization_id = app.current_organization_id());

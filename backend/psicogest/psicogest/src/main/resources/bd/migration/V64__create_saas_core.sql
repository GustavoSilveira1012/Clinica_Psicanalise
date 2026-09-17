-- v1.0: SaaS core. Organization is the tenant boundary; Clinic remains a
-- clinical/operational context inside an organization.

CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    timezone VARCHAR(80) NOT NULL DEFAULT 'America/Sao_Paulo',
    owner_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_organization_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT chk_organization_type CHECK (type IN ('SOLO', 'CLINIC', 'NETWORK')),
    CONSTRAINT chk_organization_status CHECK (status IN ('ACTIVE', 'RESTRICTED', 'SUSPENDED', 'CLOSED')),
    CONSTRAINT chk_organization_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]{2,78}[a-z0-9]$'),
    CONSTRAINT chk_organization_version CHECK (version >= 0)
);

CREATE INDEX idx_organizations_owner ON organizations(owner_user_id);
CREATE INDEX idx_organizations_status ON organizations(status);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    invited_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_organization_membership_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_membership_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_organization_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'BILLING', 'MEMBER')),
    CONSTRAINT chk_organization_membership_status CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED')),
    CONSTRAINT chk_organization_membership_version CHECK (version >= 0)
);

CREATE INDEX idx_organization_membership_user ON organization_memberships(user_id, status);
CREATE INDEX idx_organization_membership_organization ON organization_memberships(organization_id, status, role);
CREATE UNIQUE INDEX ux_organization_membership_active_user
    ON organization_memberships(organization_id, user_id)
    WHERE status IN ('INVITED', 'ACTIVE', 'SUSPENDED');

CREATE TABLE organization_invites (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    invited_by_user_id BIGINT NOT NULL,
    email_hash VARCHAR(64) NOT NULL,
    email_ciphertext BYTEA,
    email_iv BYTEA,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_organization_invite_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_invite_inviter FOREIGN KEY (invited_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_organization_invite_role CHECK (role IN ('ADMIN', 'BILLING', 'MEMBER')),
    CONSTRAINT chk_organization_invite_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_organization_invite_email ON organization_invites(organization_id, email_hash, status);
CREATE INDEX idx_organization_invite_expiry ON organization_invites(status, expires_at);

CREATE TABLE saas_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE saas_plan_versions (
    id UUID PRIMARY KEY,
    saas_plan_id UUID NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    monthly_price NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    trial_days INTEGER NOT NULL DEFAULT 14,
    published_at TIMESTAMPTZ,
    retired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_saas_plan_version_plan FOREIGN KEY (saas_plan_id) REFERENCES saas_plans(id) ON DELETE RESTRICT,
    CONSTRAINT ux_saas_plan_version UNIQUE (saas_plan_id, version),
    CONSTRAINT chk_saas_plan_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT chk_saas_plan_version_price CHECK (monthly_price >= 0),
    CONSTRAINT chk_saas_plan_version_currency CHECK (currency = 'BRL'),
    CONSTRAINT chk_saas_plan_version_trial CHECK (trial_days BETWEEN 0 AND 90)
);

CREATE TABLE saas_features (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    unit VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE saas_plan_entitlements (
    id UUID PRIMARY KEY,
    saas_plan_version_id UUID NOT NULL,
    feature_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    limit_value BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_saas_entitlement_version FOREIGN KEY (saas_plan_version_id) REFERENCES saas_plan_versions(id) ON DELETE CASCADE,
    CONSTRAINT fk_saas_entitlement_feature FOREIGN KEY (feature_id) REFERENCES saas_features(id) ON DELETE RESTRICT,
    CONSTRAINT ux_saas_entitlement UNIQUE (saas_plan_version_id, feature_id),
    CONSTRAINT chk_saas_entitlement_limit CHECK (limit_value IS NULL OR limit_value >= 0)
);

CREATE TABLE saas_subscriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    saas_plan_version_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    trial_started_at TIMESTAMPTZ,
    trial_ends_at TIMESTAMPTZ,
    current_period_start DATE,
    current_period_end DATE,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMPTZ,
    provider VARCHAR(60),
    provider_subscription_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_saas_subscription_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_saas_subscription_plan_version FOREIGN KEY (saas_plan_version_id) REFERENCES saas_plan_versions(id) ON DELETE RESTRICT,
    CONSTRAINT ux_saas_subscription_organization UNIQUE (organization_id),
    CONSTRAINT ux_saas_subscription_provider UNIQUE (provider, provider_subscription_id),
    CONSTRAINT chk_saas_subscription_status CHECK (status IN ('TRIALING', 'ACTIVE', 'PAST_DUE', 'RESTRICTED', 'CANCEL_AT_PERIOD_END', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_saas_subscription_version CHECK (version >= 0)
);

CREATE INDEX idx_saas_subscription_status ON saas_subscriptions(status, trial_ends_at);

CREATE TABLE saas_invoices (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    provider VARCHAR(60),
    provider_invoice_id VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    due_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    hosted_invoice_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_saas_invoice_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_saas_invoice_subscription FOREIGN KEY (subscription_id) REFERENCES saas_subscriptions(id) ON DELETE RESTRICT,
    CONSTRAINT ux_saas_invoice_provider UNIQUE (provider, provider_invoice_id),
    CONSTRAINT chk_saas_invoice_status CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE')),
    CONSTRAINT chk_saas_invoice_amount CHECK (amount >= 0),
    CONSTRAINT chk_saas_invoice_currency CHECK (currency = 'BRL')
);

CREATE INDEX idx_saas_invoice_organization ON saas_invoices(organization_id, due_at DESC);

CREATE TABLE saas_usage_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    feature_code VARCHAR(80) NOT NULL,
    quantity BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_saas_usage_event_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT ux_saas_usage_event_idempotency UNIQUE (organization_id, idempotency_key),
    CONSTRAINT chk_saas_usage_event_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_saas_usage_event_period ON saas_usage_events(organization_id, feature_code, occurred_at);

CREATE TABLE saas_usage_monthly (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    feature_code VARCHAR(80) NOT NULL,
    period_start DATE NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_saas_usage_monthly_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT ux_saas_usage_monthly UNIQUE (organization_id, feature_code, period_start),
    CONSTRAINT chk_saas_usage_monthly_quantity CHECK (quantity >= 0)
);

CREATE TABLE feature_flags (
    id UUID PRIMARY KEY,
    organization_id UUID,
    flag_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rollout JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_feature_flag_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT ux_feature_flag_scope UNIQUE (organization_id, flag_key)
);

CREATE TABLE organization_onboarding (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL UNIQUE,
    current_step VARCHAR(40) NOT NULL DEFAULT 'PROFILE',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_organization_onboarding_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_organization_onboarding_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

CREATE TABLE support_access_grants (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    support_user_id BIGINT NOT NULL,
    granted_by_user_id BIGINT NOT NULL,
    scopes JSONB NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_support_grant_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_grant_support_user FOREIGN KEY (support_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_support_grant_granted_by FOREIGN KEY (granted_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_support_grant_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_support_grant_period CHECK (expires_at > starts_at)
);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE terms_of_service_versions (
    id UUID PRIMARY KEY,
    version VARCHAR(40) NOT NULL UNIQUE,
    document_hash VARCHAR(64) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE organization_agreement_acceptances (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    terms_version_id UUID NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    source_ip VARCHAR(45),
    user_agent_hash VARCHAR(64),
    CONSTRAINT fk_agreement_acceptance_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_agreement_acceptance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agreement_acceptance_terms FOREIGN KEY (terms_version_id) REFERENCES terms_of_service_versions(id) ON DELETE RESTRICT,
    CONSTRAINT ux_agreement_acceptance UNIQUE (organization_id, user_id, terms_version_id)
);

CREATE INDEX idx_agreement_acceptance_organization ON organization_agreement_acceptances(organization_id, accepted_at DESC);

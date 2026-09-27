-- Keep notification processing state separate from the shared domain outbox.
-- A notification consumer must never claim or complete events for other
-- domain-event consumers (payments, packages, subscriptions, etc.).
CREATE TABLE notification_outbox_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    claimed_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_outbox_receipt_event
        FOREIGN KEY (event_id) REFERENCES domain_event_outbox(id) ON DELETE RESTRICT,
    CONSTRAINT fk_notification_outbox_receipt_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT uq_notification_outbox_receipt_tenant_event
        UNIQUE (organization_id, event_id),
    CONSTRAINT chk_notification_outbox_receipt_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'PROCESSED', 'IGNORED', 'DEAD_LETTER')),
    CONSTRAINT chk_notification_outbox_receipt_attempts
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_notification_outbox_receipt_due
    ON notification_outbox_receipts(organization_id, status, next_attempt_at, created_at);

ALTER TABLE notification_outbox_receipts ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_outbox_receipts FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_outbox_receipts_current_organization
    ON notification_outbox_receipts
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

CREATE TRIGGER trg_current_organization
    BEFORE INSERT OR UPDATE ON notification_outbox_receipts
    FOR EACH ROW EXECUTE FUNCTION app.assign_current_organization();

CREATE FUNCTION app.validate_notification_outbox_receipt_tenant() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    event_organization UUID;
BEGIN
    IF NEW.organization_id IS DISTINCT FROM app.current_organization_id() THEN
        RAISE EXCEPTION 'notification outbox receipt outside current organization'
            USING ERRCODE = '42501';
    END IF;

    SELECT organization_id INTO event_organization
      FROM domain_event_outbox
     WHERE id = NEW.event_id;

    IF event_organization IS NULL OR event_organization IS DISTINCT FROM NEW.organization_id THEN
        RAISE EXCEPTION 'notification outbox receipt event belongs to another organization'
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_validate_notification_outbox_receipt_tenant
    BEFORE INSERT OR UPDATE ON notification_outbox_receipts
    FOR EACH ROW EXECUTE FUNCTION app.validate_notification_outbox_receipt_tenant();

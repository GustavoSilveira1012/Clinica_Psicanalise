-- v1.0 production hardening: fiscal draft data and financial tenant scope.
-- All statements are additive and safe for an already migrated installation.

ALTER TABLE service_invoices ADD COLUMN IF NOT EXISTS fiscal_issuer_id UUID;
ALTER TABLE service_invoices ADD COLUMN IF NOT EXISTS competence_date DATE;
ALTER TABLE service_invoices ADD COLUMN IF NOT EXISTS layout_version VARCHAR(10);
ALTER TABLE service_invoices ADD COLUMN IF NOT EXISTS dps_number BIGINT;
ALTER TABLE service_invoices ADD COLUMN IF NOT EXISTS service_description VARCHAR(2000);
ALTER TABLE service_invoice_items ADD COLUMN IF NOT EXISTS receivable_id UUID;

UPDATE service_invoices
SET competence_date = COALESCE(competence_date, created_at::date),
    layout_version = COALESCE(layout_version, '2.03'),
    service_description = COALESCE(service_description, 'Serviço clínico'),
    fiscal_issuer_id = COALESCE(fiscal_issuer_id, (
        SELECT fi.id FROM fiscal_issuers fi WHERE fi.clinic_id = service_invoices.clinic_id AND fi.active = TRUE ORDER BY fi.created_at LIMIT 1
    ));

ALTER TABLE service_invoices ALTER COLUMN competence_date SET NOT NULL;
ALTER TABLE service_invoices ALTER COLUMN layout_version SET NOT NULL;
ALTER TABLE service_invoices ALTER COLUMN service_description SET NOT NULL;
ALTER TABLE service_invoices ADD CONSTRAINT fk_service_invoice_fiscal_issuer
    FOREIGN KEY (fiscal_issuer_id) REFERENCES fiscal_issuers(id) NOT VALID;

ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE bank_transactions ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE bank_reconciliation_allocations ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE provider_settlements ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE provider_settlement_items ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fiscal_providers ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fiscal_issuers ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fiscal_configurations ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE service_invoice_items ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fiscal_operations ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE service_invoice_tax_snapshots ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE fiscal_events ADD COLUMN IF NOT EXISTS organization_id UUID;

UPDATE bank_accounts b SET organization_id = c.organization_id FROM clinics c WHERE b.clinic_id = c.id AND b.organization_id IS NULL;
UPDATE bank_transactions t SET organization_id = b.organization_id FROM bank_accounts b WHERE t.bank_account_id = b.id AND t.organization_id IS NULL;
UPDATE bank_reconciliation_allocations a SET organization_id = t.organization_id FROM bank_transactions t WHERE a.bank_transaction_id = t.id AND a.organization_id IS NULL;
UPDATE provider_settlements s SET organization_id = c.organization_id FROM clinics c WHERE s.clinic_id = c.id AND s.organization_id IS NULL;
UPDATE provider_settlement_items i SET organization_id = s.organization_id FROM provider_settlements s WHERE i.settlement_id = s.id AND i.organization_id IS NULL;
UPDATE fiscal_providers p SET organization_id = c.organization_id FROM clinics c WHERE p.clinic_id = c.id AND p.organization_id IS NULL;
UPDATE fiscal_issuers i SET organization_id = c.organization_id FROM clinics c WHERE i.clinic_id = c.id AND i.organization_id IS NULL;
UPDATE fiscal_configurations c SET organization_id = i.organization_id FROM fiscal_issuers i WHERE c.issuer_id = i.id AND c.organization_id IS NULL;
UPDATE service_invoice_items i SET organization_id = s.organization_id FROM service_invoices s WHERE i.invoice_id = s.id AND i.organization_id IS NULL;
UPDATE fiscal_operations o SET organization_id = s.organization_id FROM service_invoices s WHERE o.invoice_id = s.id AND o.organization_id IS NULL;
UPDATE service_invoice_tax_snapshots t SET organization_id = s.organization_id FROM service_invoices s WHERE t.invoice_id = s.id AND t.organization_id IS NULL;
UPDATE fiscal_events e SET organization_id = s.organization_id FROM service_invoices s WHERE e.invoice_id = s.id AND e.organization_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_bank_accounts_organization ON bank_accounts(organization_id, clinic_id);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_organization ON bank_transactions(organization_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_fiscal_operations_organization ON fiscal_operations(organization_id, status, created_at);

-- Populate tenant context for legacy writes from their owning clinical FK.
CREATE OR REPLACE FUNCTION app.assign_organization_from_clinic() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.organization_id IS NULL AND NEW.clinic_id IS NOT NULL THEN
        SELECT organization_id INTO NEW.organization_id FROM clinics WHERE id = NEW.clinic_id;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION app.assign_organization_from_invoice() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.organization_id IS NULL AND NEW.invoice_id IS NOT NULL THEN
        SELECT organization_id INTO NEW.organization_id FROM service_invoices WHERE id = NEW.invoice_id;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION app.assign_organization_from_bank_account() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.organization_id IS NULL AND NEW.bank_account_id IS NOT NULL THEN
        SELECT organization_id INTO NEW.organization_id FROM bank_accounts WHERE id = NEW.bank_account_id;
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_bank_accounts_organization ON bank_accounts;
CREATE TRIGGER trg_bank_accounts_organization BEFORE INSERT OR UPDATE ON bank_accounts FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_clinic();
DROP TRIGGER IF EXISTS trg_provider_settlements_organization ON provider_settlements;
CREATE TRIGGER trg_provider_settlements_organization BEFORE INSERT OR UPDATE ON provider_settlements FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_clinic();
DROP TRIGGER IF EXISTS trg_fiscal_providers_organization ON fiscal_providers;
CREATE TRIGGER trg_fiscal_providers_organization BEFORE INSERT OR UPDATE ON fiscal_providers FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_clinic();
DROP TRIGGER IF EXISTS trg_fiscal_issuers_organization ON fiscal_issuers;
CREATE TRIGGER trg_fiscal_issuers_organization BEFORE INSERT OR UPDATE ON fiscal_issuers FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_clinic();
DROP TRIGGER IF EXISTS trg_service_invoice_items_organization ON service_invoice_items;
CREATE TRIGGER trg_service_invoice_items_organization BEFORE INSERT OR UPDATE ON service_invoice_items FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_invoice();
DROP TRIGGER IF EXISTS trg_fiscal_operations_organization ON fiscal_operations;
CREATE TRIGGER trg_fiscal_operations_organization BEFORE INSERT OR UPDATE ON fiscal_operations FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_invoice();
DROP TRIGGER IF EXISTS trg_service_invoice_tax_snapshots_organization ON service_invoice_tax_snapshots;
CREATE TRIGGER trg_service_invoice_tax_snapshots_organization BEFORE INSERT OR UPDATE ON service_invoice_tax_snapshots FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_invoice();
DROP TRIGGER IF EXISTS trg_fiscal_events_organization ON fiscal_events;
CREATE TRIGGER trg_fiscal_events_organization BEFORE INSERT OR UPDATE ON fiscal_events FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_invoice();
DROP TRIGGER IF EXISTS trg_bank_transactions_organization ON bank_transactions;
CREATE TRIGGER trg_bank_transactions_organization BEFORE INSERT OR UPDATE ON bank_transactions FOR EACH ROW EXECUTE FUNCTION app.assign_organization_from_bank_account();

ALTER TABLE service_invoice_items ADD CONSTRAINT fk_service_invoice_item_receivable
    FOREIGN KEY (receivable_id) REFERENCES receivables(id) ON DELETE RESTRICT NOT VALID;

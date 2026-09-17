-- v1.0 VALIDATE preparation. Constraints are NOT VALID so deployment can be
-- rolled out without a table-wide lock; validation is a separate operation.

ALTER TABLE clinics ADD CONSTRAINT fk_clinics_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE patients ADD CONSTRAINT fk_patients_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE psychoanalysts ADD CONSTRAINT fk_psychoanalysts_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE clinic_memberships ADD CONSTRAINT fk_clinic_memberships_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE clinic_user_memberships ADD CONSTRAINT fk_clinic_user_memberships_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE availability ADD CONSTRAINT fk_availability_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE therapeutic_relationships ADD CONSTRAINT fk_therapeutic_relationships_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE medical_records ADD CONSTRAINT fk_medical_records_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE medical_record_revisions ADD CONSTRAINT fk_medical_record_revisions_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE medical_record_addendums ADD CONSTRAINT fk_medical_record_addendums_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE receivables ADD CONSTRAINT fk_receivables_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE payments ADD CONSTRAINT fk_payments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE patient_packages ADD CONSTRAINT fk_patient_packages_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE patient_subscriptions ADD CONSTRAINT fk_patient_subscriptions_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE service_invoices ADD CONSTRAINT fk_service_invoices_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE fiscal_documents ADD CONSTRAINT fk_fiscal_documents_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE notification_deliveries ADD CONSTRAINT fk_notification_deliveries_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE domain_event_outbox ADD CONSTRAINT fk_domain_event_outbox_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;
ALTER TABLE data_subject_requests ADD CONSTRAINT fk_data_subject_requests_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;

CREATE INDEX idx_clinics_organization ON clinics(organization_id);
CREATE INDEX idx_patients_organization ON patients(organization_id, id);
CREATE INDEX idx_appointments_organization ON appointments(organization_id, scheduled_start);
CREATE INDEX idx_medical_records_organization ON medical_records(organization_id, patient_id);
CREATE INDEX idx_receivables_organization ON receivables(organization_id, due_date);
CREATE INDEX idx_notifications_organization ON notifications(organization_id, created_at DESC);
CREATE INDEX idx_domain_event_outbox_organization ON domain_event_outbox(organization_id, status, occurred_at);

-- Legacy RLS policies are deliberately created only after the application has
-- been migrated to set app.organization_id on every request/worker.
CREATE SCHEMA IF NOT EXISTS app;
CREATE OR REPLACE FUNCTION app.current_organization_id()
RETURNS UUID
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    raw_value TEXT;
BEGIN
    raw_value := current_setting('app.organization_id', true);
    IF raw_value IS NULL OR raw_value = '' THEN
        RETURN NULL;
    END IF;
    RETURN raw_value::UUID;
EXCEPTION WHEN invalid_text_representation THEN
    RETURN NULL;
END;
$$;

CREATE POLICY clinics_tenant_isolation ON clinics
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY patients_tenant_isolation ON patients
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY appointments_tenant_isolation ON appointments
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY medical_records_tenant_isolation ON medical_records
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY receivables_tenant_isolation ON receivables
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY notifications_tenant_isolation ON notifications
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());
CREATE POLICY domain_event_outbox_tenant_isolation ON domain_event_outbox
    USING (organization_id = app.current_organization_id())
    WITH CHECK (organization_id = app.current_organization_id());

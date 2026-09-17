-- Audit/security events are tenant-attributed for reporting and incident
-- response. Existing records were backfilled to the bootstrap organization.

ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_logs_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;

ALTER TABLE security_events
    ADD CONSTRAINT fk_security_events_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id) NOT VALID;

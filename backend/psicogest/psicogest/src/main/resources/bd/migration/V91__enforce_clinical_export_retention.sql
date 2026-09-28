-- Clinical exports are temporary artifacts; the established policy is 90 days.
ALTER TABLE clinical_exports
    DROP CONSTRAINT IF EXISTS clinical_exports_status_check;

ALTER TABLE clinical_exports
    ADD CONSTRAINT clinical_exports_status_check
        CHECK (status IN ('REQUESTED', 'PROCESSING', 'READY', 'EXPIRING', 'FAILED', 'EXPIRED'));

UPDATE clinical_exports
   SET expires_at = COALESCE(completed_at, requested_at) + INTERVAL '90 days'
 WHERE status = 'READY'
   AND storage_key IS NOT NULL
   AND expires_at IS NULL;

CREATE OR REPLACE FUNCTION app.default_clinical_export_expiry()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.status = 'READY' AND NEW.expires_at IS NULL THEN
        NEW.expires_at := COALESCE(NEW.completed_at, CURRENT_TIMESTAMP) + INTERVAL '90 days';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_clinical_export_expiry ON clinical_exports;
CREATE TRIGGER trg_clinical_export_expiry
    BEFORE INSERT OR UPDATE OF status, expires_at, completed_at
    ON clinical_exports
    FOR EACH ROW
    EXECUTE FUNCTION app.default_clinical_export_expiry();

CREATE INDEX IF NOT EXISTS idx_clinical_exports_retention_due
    ON clinical_exports (organization_id, expires_at, id)
    WHERE status IN ('READY', 'EXPIRING') AND storage_key IS NOT NULL;

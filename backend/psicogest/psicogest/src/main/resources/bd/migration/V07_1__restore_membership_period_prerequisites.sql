-- Compatibility bridge for clean databases: V08 requires the membership
-- lifecycle that was previously provisioned manually. Keep V01/V08 immutable.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'membership_status') THEN
        CREATE TYPE membership_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING');
    END IF;
END $$;

ALTER TABLE clinic_memberships ADD COLUMN IF NOT EXISTS status membership_status;
ALTER TABLE clinic_memberships ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP;
UPDATE clinic_memberships SET status = CASE WHEN active THEN 'ACTIVE'::membership_status
    ELSE 'INACTIVE'::membership_status END WHERE status IS NULL;
UPDATE clinic_memberships SET joined_at = created_at WHERE joined_at IS NULL;
ALTER TABLE clinic_memberships ALTER COLUMN status SET NOT NULL;
ALTER TABLE clinic_memberships ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE clinic_memberships ALTER COLUMN joined_at SET NOT NULL;
ALTER TABLE clinic_memberships ALTER COLUMN joined_at SET DEFAULT CURRENT_TIMESTAMP;

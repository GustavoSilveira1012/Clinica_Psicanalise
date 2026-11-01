-- A backup job records the snapshot start only after the encrypted database
-- and object archives have been uploaded and verified outside Supabase.
-- Runtime receives SELECT only; the separate backup role receives UPDATE.
CREATE TABLE app.clinical_backup_checkpoints (
    id smallint PRIMARY KEY CHECK (id = 1),
    snapshot_started_at timestamptz NOT NULL,
    confirmed_at timestamptz NOT NULL DEFAULT now() CHECK (confirmed_at >= snapshot_started_at),
    manifest_key text NOT NULL CHECK (length(manifest_key) BETWEEN 1 AND 512)
);

REVOKE ALL ON app.clinical_backup_checkpoints FROM PUBLIC;

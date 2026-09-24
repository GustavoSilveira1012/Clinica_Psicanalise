-- Both clinical readers and the append-only writer use the canonical table.
ALTER TABLE medical_record_addendums ADD COLUMN IF NOT EXISTS metadata JSONB;

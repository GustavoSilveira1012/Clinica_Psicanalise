-- Install the relationship after both sides exist on a clean database.
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
        WHERE conrelid = 'refresh_tokens'::regclass AND conname = 'fk_refresh_token_session') THEN
        ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_token_session
            FOREIGN KEY (family_id) REFERENCES user_sessions(id) ON DELETE RESTRICT;
    END IF;
END $$;

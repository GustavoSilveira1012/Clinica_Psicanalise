ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;


ALTER TABLE psychoanalysts
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;


ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;


ALTER TABLE availability
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;


ALTER TABLE users
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reactivated_at TIMESTAMP;

    CREATE INDEX IF NOT EXISTS idx_patients_active
    ON patients(active);

CREATE INDEX IF NOT EXISTS idx_psychoanalysts_active
    ON psychoanalysts(active);

CREATE INDEX IF NOT EXISTS idx_users_active
    ON users(active);

CREATE INDEX IF NOT EXISTS idx_clinics_active_lifecycle
    ON clinics(active);

CREATE INDEX IF NOT EXISTS idx_availability_active_lifecycle
    ON availability(active);
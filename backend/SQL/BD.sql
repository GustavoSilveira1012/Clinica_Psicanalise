-- PSICOGEST - PostgreSQL
-- Script idempotente: pode ser executado mais de uma vez.

SET search_path TO public, pg_catalog;

DO $$
BEGIN
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'public'::regnamespace AND typname = 'user_role') THEN
		CREATE TYPE user_role AS ENUM ('ADMIN', 'PSYCHOANALYST', 'PATIENT');
	END IF;
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'public'::regnamespace AND typname = 'appointment_status') THEN
		CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
	END IF;
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'public'::regnamespace AND typname = 'appointment_type') THEN
		CREATE TYPE appointment_type AS ENUM ('ONLINE', 'IN_PERSON');
	END IF;
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'public'::regnamespace AND typname = 'payment_status') THEN
		CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED');
	END IF;
	IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'public'::regnamespace AND typname = 'payment_method') THEN
		CREATE TYPE payment_method AS ENUM ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'CASH', 'TRANSFER');
	END IF;
END $$;

CREATE TABLE IF NOT EXISTS users (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(150) NOT NULL,
	email VARCHAR(255) NOT NULL UNIQUE,
	password_hash VARCHAR(255) NOT NULL,
	role user_role NOT NULL DEFAULT 'PATIENT',
	active BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS patients (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL UNIQUE,
	phone VARCHAR(30),
	birth_date DATE,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS psychoanalysts (
	id BIGSERIAL PRIMARY KEY,
	user_id BIGINT NOT NULL UNIQUE,
	license_number VARCHAR(100),
	specialization VARCHAR(150),
	bio TEXT,
	phone VARCHAR(30),
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_psychoanalyst_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
	id BIGSERIAL PRIMARY KEY,
	patient_id BIGINT NOT NULL,
	psychoanalyst_id BIGINT NOT NULL,
	scheduled_at TIMESTAMP NOT NULL,
	duration_minutes INTEGER NOT NULL DEFAULT 50,
	status appointment_status NOT NULL DEFAULT 'SCHEDULED',
	type appointment_type NOT NULL DEFAULT 'ONLINE',
	notes TEXT,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
	CONSTRAINT fk_appointment_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
	CONSTRAINT check_duration CHECK (duration_minutes > 0)
);

CREATE TABLE IF NOT EXISTS medical_records (
	id BIGSERIAL PRIMARY KEY,
	appointment_id BIGINT NOT NULL UNIQUE,
	patient_id BIGINT NOT NULL,
	psychoanalyst_id BIGINT NOT NULL,
	content TEXT NOT NULL,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT,
	CONSTRAINT fk_record_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
	CONSTRAINT fk_record_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS payments (
	id BIGSERIAL PRIMARY KEY,
	appointment_id BIGINT NOT NULL,
	patient_id BIGINT NOT NULL,
	amount DECIMAL(10,2) NOT NULL,
	status payment_status NOT NULL DEFAULT 'PENDING',
	payment_method payment_method,
	paid_at TIMESTAMP,
	created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT,
	CONSTRAINT fk_payment_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
	CONSTRAINT check_payment_amount CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS availability (
	id BIGSERIAL PRIMARY KEY,
	psychoanalyst_id BIGINT NOT NULL,
	day_of_week INTEGER NOT NULL,
	start_time TIME NOT NULL,
	end_time TIME NOT NULL,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	CONSTRAINT fk_availability_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE CASCADE,
	CONSTRAINT check_day_of_week CHECK (day_of_week BETWEEN 0 AND 6),
	CONSTRAINT check_availability_time CHECK (end_time > start_time)
);

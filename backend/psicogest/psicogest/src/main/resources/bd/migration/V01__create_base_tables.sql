-- V01: Create base tables and enums
-- Initial schema with users, patients, psychoanalysts, appointments, and related tables

CREATE TYPE user_role AS ENUM ('ADMIN', 'PSYCHOANALYST', 'PATIENT', 'CLINIC_ADMIN', 'SYSTEM_ADMIN');
CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
CREATE TYPE appointment_type AS ENUM ('ONLINE', 'IN_PERSON');
CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'CASH', 'TRANSFER');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'PATIENT',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(30),
    birth_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    deactivation_reason VARCHAR(255),
    reactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE psychoanalysts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    license_number VARCHAR(100),
    specialization VARCHAR(150),
    bio TEXT,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    deactivation_reason VARCHAR(255),
    reactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_psychoanalyst_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE clinics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    deactivation_reason VARCHAR(255),
    reactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinic_memberships (
    id BIGSERIAL PRIMARY KEY,
    clinic_id BIGINT NOT NULL,
    psychoanalyst_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deactivated_at TIMESTAMP,
    deactivation_reason VARCHAR(255),
    reactivated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
    UNIQUE(clinic_id, psychoanalyst_id)
);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    psychoanalyst_id BIGINT NOT NULL,
    clinic_membership_id BIGINT,
    original_appointment_id BIGINT,
    recurring_group_id UUID,
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP NOT NULL,
    status appointment_status NOT NULL DEFAULT 'SCHEDULED',
    appointment_type appointment_type NOT NULL DEFAULT 'ONLINE',
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_clinic_membership FOREIGN KEY (clinic_membership_id) REFERENCES clinic_memberships(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_original FOREIGN KEY (original_appointment_id) REFERENCES appointments(id) ON DELETE RESTRICT,
    CONSTRAINT check_appointment_time CHECK (scheduled_end > scheduled_start)
);

CREATE TABLE medical_records (
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

CREATE TABLE payments (
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

CREATE TABLE availability (
    id BIGSERIAL PRIMARY KEY,
    psychoanalyst_id BIGINT NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_availability_psychoanalyst FOREIGN KEY (psychoanalyst_id) REFERENCES psychoanalysts(id) ON DELETE CASCADE,
    CONSTRAINT check_availability_time CHECK (end_time > start_time),
    CONSTRAINT check_day_of_week CHECK (day_of_week >= 0 AND day_of_week <= 6)
);

CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_psychoanalysts_user_id ON psychoanalysts(user_id);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_psychoanalyst_id ON appointments(psychoanalyst_id);
CREATE INDEX idx_appointments_scheduled_start ON appointments(scheduled_start);
CREATE INDEX idx_availability_psychoanalyst_id ON availability(psychoanalyst_id);

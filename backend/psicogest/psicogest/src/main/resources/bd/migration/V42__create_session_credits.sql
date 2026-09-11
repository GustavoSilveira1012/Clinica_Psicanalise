-- Tabela de pacotes de sessão do paciente
CREATE TABLE patient_packages (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    package_plan_version_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    activation_date DATE,
    expiration_date DATE,
    available_sessions BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    FOREIGN KEY (patient_id) REFERENCES users(id),
    INDEX idx_patient_package_patient (patient_id),
    INDEX idx_patient_package_status (status),
    INDEX idx_patient_package_created_at (created_at)
);

-- Tabela de ledger de créditos de sessão
CREATE TABLE session_credit_entries (
    id UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    patient_package_id UUID,
    entry_type VARCHAR(50) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    session_count BIGINT NOT NULL,
    reason VARCHAR(500),
    appointment_id UUID,
    package_item_id UUID,
    created_at TIMESTAMP NOT NULL,
    
    FOREIGN KEY (patient_id) REFERENCES users(id),
    FOREIGN KEY (patient_package_id) REFERENCES patient_packages(id),
    INDEX idx_credit_entry_patient (patient_id),
    INDEX idx_credit_entry_package (patient_package_id),
    INDEX idx_credit_entry_type (entry_type),
    INDEX idx_credit_entry_direction (direction),
    INDEX idx_credit_entry_created_at (created_at)
);

-- Tabela de consumos de pacote (rastreamento de qual sessão foi consumida)
CREATE TABLE package_consumptions (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    patient_package_id UUID NOT NULL,
    package_item_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    consumed_at TIMESTAMP NOT NULL,
    reversed_at TIMESTAMP,
    reversal_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (patient_package_id) REFERENCES patient_packages(id),
    INDEX idx_consumption_appointment (appointment_id),
    INDEX idx_consumption_package (patient_package_id),
    INDEX idx_consumption_item (package_item_id),
    INDEX idx_consumption_status (status),
    INDEX idx_consumption_created_at (created_at)
);

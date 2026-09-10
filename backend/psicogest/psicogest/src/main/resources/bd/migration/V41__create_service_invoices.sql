-- V41: Criar tabelas de notas fiscais de serviço

CREATE TABLE service_invoices (
    id UUID PRIMARY KEY,
    clinic_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    environment VARCHAR(50) NOT NULL,
    tax_regime VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    invoice_number VARCHAR(50),
    nfse_id VARCHAR(100) UNIQUE,
    gross_amount NUMERIC(19, 2) NOT NULL,
    deductions NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,
    authorized_at TIMESTAMP,
    rejected_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    updated_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    version BIGINT,
    
    CONSTRAINT fk_service_invoice_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_service_invoice_status
        CHECK (status IN (
            'DRAFT',
            'PENDING',
            'PROCESSING',
            'AUTHORIZED',
            'REJECTED',
            'CANCEL_PENDING',
            'CANCELLED',
            'ERROR',
            'RECONCILIATION_REQUIRED'
        ))
);

CREATE INDEX idx_service_invoice_clinic ON service_invoices (clinic_id);
CREATE INDEX idx_service_invoice_status ON service_invoices (status);
CREATE INDEX idx_service_invoice_nfse_id ON service_invoices (nfse_id);
CREATE INDEX idx_service_invoice_created_at ON service_invoices (created_at);

COMMENT ON TABLE service_invoices IS 'Notas fiscais de serviço eletrônicas (NFS-e)';

-- Itens da nota fiscal
CREATE TABLE service_invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    service_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_service_invoice_item_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE CASCADE,
    
    CONSTRAINT chk_service_invoice_item_type
        CHECK (item_type IN (
            'SERVICE',
            'DEDUCTION',
            'ADDITIONAL_CHARGE'
        ))
);

CREATE INDEX idx_service_invoice_item_invoice ON service_invoice_items (invoice_id);
CREATE INDEX idx_service_invoice_item_type ON service_invoice_items (item_type);

COMMENT ON TABLE service_invoice_items IS 'Itens que compõem uma nota fiscal de serviço';

-- Operações fiscais (emissão, cancelamento, substituição)
CREATE TABLE fiscal_operations (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    request_payload TEXT,
    response_payload TEXT,
    error_message VARCHAR(1000),
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    version BIGINT,
    
    CONSTRAINT fk_fiscal_operation_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES service_invoices(id)
        ON DELETE CASCADE,
    
    CONSTRAINT chk_fiscal_operation_type
        CHECK (operation_type IN ('ISSUE', 'CANCEL', 'SUBSTITUTE')),
    
    CONSTRAINT chk_fiscal_operation_status
        CHECK (status IN ('PENDING', 'CLAIMED', 'PROCESSING', 'COMPLETED', 'FAILED', 'INDETERMINATE'))
);

CREATE INDEX idx_fiscal_operation_invoice ON fiscal_operations (invoice_id);
CREATE INDEX idx_fiscal_operation_type_status ON fiscal_operations (operation_type, status);
CREATE INDEX idx_fiscal_operation_idempotency ON fiscal_operations (idempotency_key);

COMMENT ON TABLE fiscal_operations IS 'Rastreamento de operações fiscais (emissão, cancelamento, substituição)';

-- V36: Criar tabelas de repasse de gateway

CREATE TABLE provider_settlements (
    id UUID PRIMARY KEY,
    clinic_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_settlement_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    validation_status VARCHAR(50) NOT NULL,
    settlement_direction VARCHAR(50),
    reported_net_amount NUMERIC(19, 2),
    currency VARCHAR(3) NOT NULL,
    expected_at TIMESTAMP,
    settled_at TIMESTAMP,
    validated_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_code VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT,
    
    CONSTRAINT fk_settlement_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_settlement_status
        CHECK (status IN ('RECEIVED', 'VALIDATED', 'SETTLED', 'MISMATCHED')),
    
    CONSTRAINT chk_settlement_validation
        CHECK (validation_status IN ('PENDING', 'MATCHED', 'MISMATCHED', 'REMATCH')),
    
    CONSTRAINT chk_settlement_direction
        CHECK (settlement_direction IN ('CREDIT_TO_FINANCIAL_ENTITY', 'DEBIT_FROM_FINANCIAL_ENTITY'))
);

CREATE INDEX idx_settlement_clinic_status ON provider_settlements (clinic_id, status);
CREATE INDEX idx_settlement_provider ON provider_settlements (provider);
CREATE INDEX idx_settlement_created_at ON provider_settlements (created_at);

COMMENT ON TABLE provider_settlements IS 'Repasse recebido do gateway (Mercado Pago, Stripe, etc) com pagamentos, refunds e taxas';
COMMENT ON COLUMN provider_settlements.provider_settlement_id IS 'ID único do repasse no gateway';
COMMENT ON COLUMN provider_settlements.status IS 'Ciclo de vida: RECEIVED → VALIDATED → SETTLED';
COMMENT ON COLUMN provider_settlements.validation_status IS 'Resultado da validação de saldo';
COMMENT ON COLUMN provider_settlements.reported_net_amount IS 'Valor líquido informado pelo gateway';

-- Tabela de itens do repasse
CREATE TABLE provider_settlement_items (
    id UUID PRIMARY KEY,
    settlement_id UUID NOT NULL,
    provider_item_id VARCHAR(100),
    item_type VARCHAR(50) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_id UUID,
    refund_id UUID,
    external_reference VARCHAR(255),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_item_settlement
        FOREIGN KEY (settlement_id)
        REFERENCES provider_settlements(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_item_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE SET NULL,
    
    CONSTRAINT fk_item_refund
        FOREIGN KEY (refund_id)
        REFERENCES refunds(id)
        ON DELETE SET NULL,
    
    CONSTRAINT chk_item_type
        CHECK (item_type IN ('PAYMENT', 'REFUND', 'PROVIDER_FEE', 'CHARGEBACK', 'ADJUSTMENT')),
    
    CONSTRAINT chk_item_direction
        CHECK (direction IN ('CREDIT', 'DEBIT')),
    
    CONSTRAINT chk_item_target
        CHECK (
            (item_type = 'PAYMENT' AND payment_id IS NOT NULL AND refund_id IS NULL)
            OR
            (item_type = 'REFUND' AND refund_id IS NOT NULL AND payment_id IS NULL)
            OR
            (item_type IN ('PROVIDER_FEE', 'CHARGEBACK', 'ADJUSTMENT') AND payment_id IS NULL AND refund_id IS NULL)
        )
);

CREATE INDEX idx_item_settlement ON provider_settlement_items (settlement_id);
CREATE INDEX idx_item_payment ON provider_settlement_items (payment_id);
CREATE INDEX idx_item_refund ON provider_settlement_items (refund_id);
CREATE INDEX idx_item_type ON provider_settlement_items (item_type);

COMMENT ON TABLE provider_settlement_items IS 'Itens que compõem um repasse: pagamentos, refunds, taxas, chargebacks, ajustes';
COMMENT ON COLUMN provider_settlement_items.item_type IS 'PAYMENT (crédito), REFUND (débito), PROVIDER_FEE (débito), CHARGEBACK (débito), ADJUSTMENT (flexível)';
COMMENT ON COLUMN provider_settlement_items.direction IS 'CREDIT (aumenta saldo) ou DEBIT (diminui saldo)';

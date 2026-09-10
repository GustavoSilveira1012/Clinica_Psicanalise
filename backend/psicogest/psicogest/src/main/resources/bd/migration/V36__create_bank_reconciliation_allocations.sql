-- V36__create_bank_reconciliation_allocations.sql
-- Criar tabela de alocações de reconciliação

CREATE TABLE bank_reconciliation_allocations (
    id UUID PRIMARY KEY NOT NULL,
    bank_transaction_id UUID NOT NULL REFERENCES bank_transactions(id),
    payment_id UUID REFERENCES payments(id),
    refund_id UUID REFERENCES refunds(id),
    allocated_amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUGGESTED' CHECK (status IN ('SUGGESTED', 'CONFIRMED', 'REVERTED', 'DISPUTED')),
    allocated_by VARCHAR(50) NOT NULL DEFAULT 'MANUAL' CHECK (allocated_by IN ('AUTOMATIC', 'MANUAL', 'SYSTEM')),
    allocated_at TIMESTAMP NOT NULL,
    
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Constraints: pelo menos um de payment_id ou refund_id deve estar preenchido
ALTER TABLE bank_reconciliation_allocations
ADD CONSTRAINT chk_at_least_one_document CHECK (
    (payment_id IS NOT NULL) OR (refund_id IS NOT NULL)
);

-- Índices
CREATE INDEX idx_bank_transaction_allocation ON bank_reconciliation_allocations(bank_transaction_id);
CREATE INDEX idx_payment_allocation ON bank_reconciliation_allocations(payment_id);
CREATE INDEX idx_refund_allocation ON bank_reconciliation_allocations(refund_id);
CREATE INDEX idx_allocation_status ON bank_reconciliation_allocations(status);
CREATE INDEX idx_allocation_source ON bank_reconciliation_allocations(allocated_by);

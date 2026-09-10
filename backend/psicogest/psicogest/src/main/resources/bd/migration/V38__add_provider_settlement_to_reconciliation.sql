-- V38: Adicionar suporte a ProviderSettlement em bank_reconciliation_allocations
-- Agora suporta: Payment, Refund, ou ProviderSettlement

-- Remover constraint antiga
ALTER TABLE bank_reconciliation_allocations
    DROP CONSTRAINT chk_bank_reconciliation_target;

-- Adicionar coluna para ProviderSettlement
ALTER TABLE bank_reconciliation_allocations
    ADD COLUMN provider_settlement_id UUID;

-- Adicionar FK
ALTER TABLE bank_reconciliation_allocations
    ADD CONSTRAINT fk_bank_reconciliation_settlement
    FOREIGN KEY (provider_settlement_id)
    REFERENCES provider_settlements(id)
    ON DELETE RESTRICT;

-- Nova constraint: exatamente um dos três deve ser NOT NULL
ALTER TABLE bank_reconciliation_allocations
    ADD CONSTRAINT chk_bank_reconciliation_target
    CHECK (
        (
            payment_id IS NOT NULL
            AND refund_id IS NULL
            AND provider_settlement_id IS NULL
        )
        OR
        (
            payment_id IS NULL
            AND refund_id IS NOT NULL
            AND provider_settlement_id IS NULL
        )
        OR
        (
            payment_id IS NULL
            AND refund_id IS NULL
            AND provider_settlement_id IS NOT NULL
        )
    );

-- Índices para performance
CREATE INDEX idx_bank_reconciliation_settlement
    ON bank_reconciliation_allocations (provider_settlement_id);

COMMENT ON COLUMN bank_reconciliation_allocations.provider_settlement_id IS 'Repasse de gateway se for reconciliação de settlement';

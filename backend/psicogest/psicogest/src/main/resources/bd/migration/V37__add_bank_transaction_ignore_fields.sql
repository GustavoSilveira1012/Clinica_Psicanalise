-- V37: Adicionar campos para ignorar lançamentos (tarifa, juros, transferências internas)

ALTER TABLE bank_transactions
    ADD COLUMN ignored_at TIMESTAMP NULL,
    ADD COLUMN ignored_by UUID NULL,
    ADD COLUMN ignore_reason VARCHAR(255) NULL;

-- Índice para filtrar não-ignorados
CREATE INDEX idx_bank_transaction_ignored_at
    ON bank_transactions (ignored_at)
    WHERE ignored_at IS NULL;

COMMENT ON COLUMN bank_transactions.ignored_at IS 'Data em que o lançamento foi marcado para ignorar';
COMMENT ON COLUMN bank_transactions.ignored_by IS 'ID do usuário que ignorou';
COMMENT ON COLUMN bank_transactions.ignore_reason IS 'Motivo da ignoração (tarifa, juros, etc)';

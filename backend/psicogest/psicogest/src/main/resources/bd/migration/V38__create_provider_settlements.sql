CREATE TABLE provider_settlements (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    provider VARCHAR(50) NOT NULL,

    provider_settlement_id VARCHAR(255) NOT NULL,

    status VARCHAR(40) NOT NULL,

    validation_status VARCHAR(30) NOT NULL,

    settlement_direction VARCHAR(50),

    reported_net_amount NUMERIC(19,2),

    currency CHAR(3) NOT NULL DEFAULT 'BRL',

    expected_at TIMESTAMPTZ,

    settled_at TIMESTAMPTZ,

    validated_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    failure_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_provider_settlement_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_provider_settlement_status
        CHECK (
            status IN (
                'RECEIVED',
                'VALIDATED',
                'SETTLED',
                'PARTIALLY_RECONCILED',
                'RECONCILED',
                'FAILED'
            )
        ),

    CONSTRAINT chk_provider_settlement_validation
        CHECK (
            validation_status IN (
                'PENDING',
                'MATCHED',
                'MISMATCHED'
            )
        ),

    CONSTRAINT chk_provider_settlement_direction
        CHECK (
            settlement_direction IS NULL
            OR
            settlement_direction IN (
                'CREDIT_TO_FINANCIAL_ENTITY',
                'DEBIT_FROM_FINANCIAL_ENTITY'
            )
        ),

    CONSTRAINT chk_provider_settlement_net
        CHECK (
            reported_net_amount IS NULL
            OR reported_net_amount >= 0
        ),

    CONSTRAINT chk_provider_settlement_currency
        CHECK (currency = 'BRL')
);

CREATE UNIQUE INDEX ux_provider_settlement_external

ON provider_settlements(
    financial_entity_id,
    provider,
    provider_settlement_id
);

-------------------------------------------------------------------------------------------------

CREATE TABLE provider_settlement_items (

    id UUID PRIMARY KEY,

    settlement_id UUID NOT NULL,

    provider_item_id VARCHAR(255),

    item_type VARCHAR(40) NOT NULL,

    direction VARCHAR(10) NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    payment_id UUID,

    refund_id UUID,

    external_reference VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_settlement_item_settlement
        FOREIGN KEY (settlement_id)
        REFERENCES provider_settlements(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_settlement_item_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_settlement_item_refund
        FOREIGN KEY (refund_id)
        REFERENCES refunds(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_settlement_item_amount
        CHECK (amount > 0),

    CONSTRAINT chk_settlement_item_direction
        CHECK (
            direction IN (
                'CREDIT',
                'DEBIT'
            )
        ),

    CONSTRAINT chk_settlement_item_type
        CHECK (
            item_type IN (
                'PAYMENT',
                'REFUND',
                'PROVIDER_FEE',
                'CHARGEBACK',
                'ADJUSTMENT'
            )
        ),

    CONSTRAINT chk_settlement_item_reference
        CHECK (
            (
                item_type = 'PAYMENT'
                AND payment_id IS NOT NULL
                AND refund_id IS NULL
            )
            OR
            (
                item_type = 'REFUND'
                AND refund_id IS NOT NULL
                AND payment_id IS NULL
            )
            OR
            (
                item_type IN (
                    'PROVIDER_FEE',
                    'CHARGEBACK',
                    'ADJUSTMENT'
                )
            )
        )
);

CREATE UNIQUE INDEX ux_settlement_provider_item

ON provider_settlement_items(
    settlement_id,
    provider_item_id
)

WHERE provider_item_id IS NOT NULL;

--------------------------------------------------------------------------

CREATE INDEX idx_settlement_item_payment
ON provider_settlement_items(payment_id);

CREATE INDEX idx_settlement_item_refund
ON provider_settlement_items(refund_id);

CREATE INDEX idx_settlement_items_settlement
ON provider_settlement_items(
    settlement_id,
    created_at
);

---------------------------------------------------------------------

CREATE OR REPLACE FUNCTION prevent_provider_settlement_item_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    RAISE EXCEPTION
        'provider settlement items are append-only';

END;
$$;


CREATE TRIGGER trg_provider_settlement_item_update
BEFORE UPDATE
ON provider_settlement_items
FOR EACH ROW
EXECUTE FUNCTION prevent_provider_settlement_item_mutation();


CREATE TRIGGER trg_provider_settlement_item_delete
BEFORE DELETE
ON provider_settlement_items
FOR EACH ROW
EXECUTE FUNCTION prevent_provider_settlement_item_mutation();
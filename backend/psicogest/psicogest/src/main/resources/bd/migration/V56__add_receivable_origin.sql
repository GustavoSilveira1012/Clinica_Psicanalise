ALTER TABLE receivables
ADD COLUMN origin_type VARCHAR(40),
ADD COLUMN origin_id UUID;

CREATE INDEX idx_receivable_origin
ON receivables(origin_type, origin_id);

CREATE UNIQUE INDEX ux_receivable_subscription_cycle
ON receivables(origin_id)
WHERE origin_type = 'SUBSCRIPTION_CYCLE';

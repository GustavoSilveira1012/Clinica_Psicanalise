-- Monetary entities use VARCHAR(3) in the Java model. Normalize legacy
-- CHAR(3) columns without changing their values or their BRL constraints.
ALTER TABLE receivables
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency);

ALTER TABLE payments
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency);

ALTER TABLE refunds
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE credit_accounts
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE package_plan_versions
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE subscription_plan_versions
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE subscription_cycles
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE saas_plan_versions
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

ALTER TABLE saas_invoices
    ALTER COLUMN currency DROP DEFAULT,
    ALTER COLUMN currency TYPE VARCHAR(3) USING rtrim(currency),
    ALTER COLUMN currency SET DEFAULT 'BRL';

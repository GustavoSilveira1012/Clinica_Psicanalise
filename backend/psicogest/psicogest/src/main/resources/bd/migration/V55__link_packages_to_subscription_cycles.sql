ALTER TABLE patient_packages
ADD COLUMN source VARCHAR(40)
NOT NULL DEFAULT 'ONE_TIME_PURCHASE';


ALTER TABLE patient_packages
ADD COLUMN purchase_amount
NUMERIC(19,2);


ALTER TABLE patient_packages
ADD COLUMN subscription_cycle_id UUID;


ALTER TABLE patient_packages
ADD CONSTRAINT fk_patient_package_subscription_cycle
FOREIGN KEY (subscription_cycle_id)
REFERENCES subscription_cycles(id)
ON DELETE RESTRICT;


ALTER TABLE patient_packages
ADD CONSTRAINT chk_patient_package_source
CHECK (
    source IN (
        'ONE_TIME_PURCHASE',
        'SUBSCRIPTION_CYCLE'
    )
);

ALTER TABLE patient_packages
ADD CONSTRAINT chk_patient_package_subscription_source
CHECK (
    (source = 'SUBSCRIPTION_CYCLE' AND subscription_cycle_id IS NOT NULL)
    OR
    (source = 'ONE_TIME_PURCHASE' AND subscription_cycle_id IS NULL)
);


CREATE UNIQUE INDEX ux_patient_package_subscription_cycle
ON patient_packages(subscription_cycle_id)
WHERE subscription_cycle_id IS NOT NULL;

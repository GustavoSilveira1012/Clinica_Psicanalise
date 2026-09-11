CREATE TABLE package_plan_items (

    id UUID PRIMARY KEY,

    package_plan_version_id UUID NOT NULL,

    service_code VARCHAR(100) NOT NULL,

    appointment_type VARCHAR(30),

    quantity INTEGER NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_package_item_version
        FOREIGN KEY (package_plan_version_id)
        REFERENCES package_plan_versions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_package_item_quantity
        CHECK (
            quantity > 0
        ),

    CONSTRAINT chk_package_item_appointment_type
        CHECK (
            appointment_type IS NULL
            OR appointment_type IN (
                'IN_PERSON',
                'ONLINE'
            )
        )
);
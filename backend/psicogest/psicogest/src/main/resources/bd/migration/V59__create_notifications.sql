CREATE TABLE notifications (

    id UUID PRIMARY KEY,

    notification_type VARCHAR(60) NOT NULL,

    aggregate_type VARCHAR(100),

    aggregate_id VARCHAR(100),

    deduplication_key VARCHAR(255) NOT NULL,

    status VARCHAR(30) NOT NULL,

    scheduled_for TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ux_notification_dedupe
        UNIQUE (deduplication_key),

    CONSTRAINT chk_notification_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PARTIALLY_DELIVERED',
                'DELIVERED',
                'FAILED',
                'CANCELLED'
            )
        )
);

---------------------------------------------------

CREATE TABLE notification_recipients (

    id UUID PRIMARY KEY,

    notification_id UUID NOT NULL,

    user_id BIGINT,

    patient_id BIGINT,

    recipient_type VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notification_recipient_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_notification_recipient_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_notification_recipient_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notification_recipient_type
        CHECK (
            recipient_type IN (
                'USER',
                'PATIENT'
            )
        )
);

---------------------------------------------

CREATE TABLE notification_templates (

    id UUID PRIMARY KEY,

    template_key VARCHAR(100) NOT NULL,

    version INTEGER NOT NULL,

    channel VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL,

    subject_template VARCHAR(255),

    body_template TEXT NOT NULL,

    locale VARCHAR(20) NOT NULL DEFAULT 'pt-BR',

    created_at TIMESTAMPTZ NOT NULL,

    published_at TIMESTAMPTZ,

    retired_at TIMESTAMPTZ,

    CONSTRAINT ux_notification_template_version
        UNIQUE (
            template_key,
            version,
            channel,
            locale
        ),

    CONSTRAINT chk_notification_template_channel
        CHECK (
            channel IN (
                'EMAIL',
                'WHATSAPP',
                'SMS'
            )
        ),

    CONSTRAINT chk_notification_template_status
        CHECK (
            status IN (
                'DRAFT',
                'PUBLISHED',
                'RETIRED'
            )
        )
);

-------------------------------------------------

CREATE TABLE notification_deliveries (

    id UUID PRIMARY KEY,

    notification_id UUID NOT NULL,

    recipient_id UUID NOT NULL,

    template_id UUID NOT NULL,

    channel VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL,

    provider VARCHAR(50),

    provider_message_id VARCHAR(255),

    destination_hash VARCHAR(64) NOT NULL,

    encrypted_destination BYTEA NOT NULL,

    destination_iv BYTEA NOT NULL,

    encrypted_destination_dek BYTEA NOT NULL,

    destination_key_id VARCHAR(255) NOT NULL,

    destination_crypto_version INTEGER NOT NULL,

    destination_crypto_algorithm VARCHAR(30) NOT NULL,

    attempt_count INTEGER NOT NULL DEFAULT 0,

    scheduled_for TIMESTAMPTZ,

    processing_started_at TIMESTAMPTZ,

    sent_at TIMESTAMPTZ,

    delivered_at TIMESTAMPTZ,

    failed_at TIMESTAMPTZ,

    next_retry_at TIMESTAMPTZ,

    last_error_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notification_delivery_notification
        FOREIGN KEY (notification_id)
        REFERENCES notifications(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_notification_delivery_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES notification_recipients(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_notification_delivery_template
        FOREIGN KEY (template_id)
        REFERENCES notification_templates(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notification_delivery_channel
        CHECK (
            channel IN (
                'EMAIL',
                'WHATSAPP',
                'SMS'
            )
        ),

    CONSTRAINT chk_notification_delivery_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'DELIVERED',
                'FAILED',
                'CANCELLED',
                'SUPPRESSED',
                'DEAD_LETTER'
            )
        )
);

----------------------------------------------------

CREATE TABLE notification_preferences (

    id UUID PRIMARY KEY,

    user_id BIGINT,

    patient_id BIGINT,

    notification_type VARCHAR(60) NOT NULL,

    channel VARCHAR(20) NOT NULL,

    enabled BOOLEAN NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notification_preference_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_notification_preference_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notification_preference_owner
        CHECK (
            (
                user_id IS NOT NULL
                AND patient_id IS NULL
            )
            OR
            (
                user_id IS NULL
                AND patient_id IS NOT NULL
            )
        )
);

----------------------------------------------------

ALTER TABLE notification_deliveries

ADD COLUMN encrypted_variables BYTEA;

ALTER TABLE notification_deliveries
ADD COLUMN variables_iv BYTEA;

ALTER TABLE notification_deliveries
ADD COLUMN encrypted_variables_dek BYTEA;

ALTER TABLE notification_deliveries
ADD COLUMN variables_key_id VARCHAR(255);

ALTER TABLE notification_deliveries
ADD COLUMN variables_crypto_version INTEGER;

ALTER TABLE notification_deliveries
ADD COLUMN variables_crypto_algorithm VARCHAR(30);

---------------------------------------------------------

CREATE TABLE notification_provider_configurations (

    id UUID PRIMARY KEY,

    financial_entity_id UUID NOT NULL,

    channel VARCHAR(20) NOT NULL,

    provider VARCHAR(50) NOT NULL,

    credential_ref VARCHAR(500) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    configuration JSONB NOT NULL
        DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notification_provider_financial_entity
        FOREIGN KEY (financial_entity_id)
        REFERENCES financial_entities(id)
        ON DELETE RESTRICT,

    CONSTRAINT ux_notification_provider_channel
        UNIQUE (
            financial_entity_id,
            channel
        )
);
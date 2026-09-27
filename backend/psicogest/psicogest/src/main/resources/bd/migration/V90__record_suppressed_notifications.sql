-- Keep pilot events visible and auditable when no outbound channel is configured.
ALTER TABLE notifications
    ADD COLUMN suppression_reason_code VARCHAR(80);

ALTER TABLE notifications
    DROP CONSTRAINT chk_notification_status;

ALTER TABLE notifications
    ADD CONSTRAINT chk_notification_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'PARTIALLY_DELIVERED',
            'DELIVERED',
            'FAILED',
            'CANCELLED',
            'SUPPRESSED'
        ));

ALTER TABLE notifications
    ADD CONSTRAINT chk_notification_suppression_reason
        CHECK (
            (status = 'SUPPRESSED' AND suppression_reason_code IS NOT NULL)
            OR (status <> 'SUPPRESSED' AND suppression_reason_code IS NULL)
        );

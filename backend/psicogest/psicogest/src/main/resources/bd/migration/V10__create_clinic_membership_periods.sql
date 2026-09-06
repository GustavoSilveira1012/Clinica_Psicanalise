CREATE TABLE clinic_membership_periods (

    id BIGSERIAL PRIMARY KEY,

    clinic_membership_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    started_at TIMESTAMP NOT NULL,

    ended_at TIMESTAMP,

    end_reason VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clinic_membership_period_membership
        FOREIGN KEY (clinic_membership_id)
        REFERENCES clinic_memberships(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_clinic_membership_period_status
        CHECK (
            status IN (
                'ACTIVE',
                'ENDED'
            )
        ),

    CONSTRAINT chk_clinic_membership_period_dates
        CHECK (
            ended_at IS NULL
            OR ended_at > started_at
        ),

    CONSTRAINT chk_clinic_membership_period_end
        CHECK (
            (
                status = 'ACTIVE'
                AND ended_at IS NULL
            )
            OR
            (
                status = 'ENDED'
                AND ended_at IS NOT NULL
            )
        )
);

CREATE INDEX idx_clinic_membership_period_membership
    ON clinic_membership_periods(
        clinic_membership_id
    );


CREATE INDEX idx_clinic_membership_period_started_at
    ON clinic_membership_periods(
        started_at
    );

    CREATE UNIQUE INDEX ux_clinic_membership_active_period
    ON clinic_membership_periods(
        clinic_membership_id
    )
    WHERE status = 'ACTIVE';

ALTER TABLE clinic_membership_periods

ADD CONSTRAINT ex_clinic_membership_period_no_overlap

EXCLUDE USING gist (

    clinic_membership_id WITH =,

    tsrange(
        started_at,

        COALESCE(
            ended_at,
            'infinity'::timestamp
        ),

        '[)'
    ) WITH &&

);

INSERT INTO clinic_membership_periods (
    clinic_membership_id,
    status,
    started_at,
    created_at,
    updated_at
)
SELECT
    id,
    'ACTIVE',
    joined_at,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM clinic_memberships
WHERE status = 'ACTIVE';
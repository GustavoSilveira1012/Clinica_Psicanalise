CREATE TABLE clinic_user_memberships (

    id BIGSERIAL PRIMARY KEY,

    clinic_id BIGINT NOT NULL,

    user_id BIGINT NOT NULL,

    access_role VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL,

    started_at TIMESTAMP NOT NULL,

    ended_at TIMESTAMP,

    end_reason VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_clinic_user_membership_clinic
        FOREIGN KEY (clinic_id)
        REFERENCES clinics(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_clinic_user_membership_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_clinic_access_role
        CHECK (
            access_role IN (
                'ADMIN'
            )
        ),

    CONSTRAINT chk_clinic_user_membership_status
        CHECK (
            status IN (
                'ACTIVE',
                'ENDED'
            )
        ),

    CONSTRAINT chk_clinic_user_membership_dates
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


CREATE UNIQUE INDEX ux_clinic_user_active_access
    ON clinic_user_memberships(
        clinic_id,
        user_id,
        access_role
    )
    WHERE status = 'ACTIVE';


CREATE INDEX idx_clinic_user_membership_user
    ON clinic_user_memberships(user_id);

CREATE INDEX idx_clinic_user_membership_clinic
    ON clinic_user_memberships(clinic_id);
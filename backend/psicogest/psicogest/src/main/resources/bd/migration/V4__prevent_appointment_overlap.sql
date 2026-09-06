CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments
ADD CONSTRAINT ex_appointments_psychoanalyst_no_overlap
EXCLUDE USING gist (

    psychoanalyst_id WITH =,

    tsrange(
        scheduled_start,
        scheduled_end,
        '[)'
    ) WITH &&

)
WHERE (
    status IN (
        'SCHEDULED',
        'CONFIRMED'
    )
);
WITH legacy_stats AS (
    SELECT
        recurring_group_id,
        MIN(scheduled_start::date) AS starts_on,
        MAX(scheduled_start::date) AS ends_on,
        COUNT(*)::integer AS total_occurrences,
        COUNT(*) FILTER (
            WHERE status IN ('SCHEDULED', 'CONFIRMED')
        ) AS active_count,
        COUNT(*) FILTER (
            WHERE status = 'COMPLETED'
        ) AS completed_count
    FROM appointments
    WHERE recurring_group_id IS NOT NULL
    GROUP BY recurring_group_id
), legacy_base AS (
    SELECT DISTINCT ON (recurring_group_id)
        recurring_group_id,
        patient_id,
        psychoanalyst_id,
        clinic_membership_id,
        scheduled_start,
        scheduled_end
    FROM appointments
    WHERE recurring_group_id IS NOT NULL
    ORDER BY recurring_group_id, scheduled_start, id
)
INSERT INTO appointment_series (
    id,
    patient_id,
    psychoanalyst_id,
    clinic_membership_id,
    frequency,
    recurrence_interval,
    day_of_week,
    start_time,
    duration_minutes,
    starts_on,
    ends_on,
    total_occurrences,
    status,
    created_at,
    updated_at
)
SELECT
    base.recurring_group_id,
    base.patient_id,
    base.psychoanalyst_id,
    base.clinic_membership_id,
    'WEEKLY',
    1,
    CASE EXTRACT(ISODOW FROM base.scheduled_start)::integer
        WHEN 1 THEN 'MONDAY'
        WHEN 2 THEN 'TUESDAY'
        WHEN 3 THEN 'WEDNESDAY'
        WHEN 4 THEN 'THURSDAY'
        WHEN 5 THEN 'FRIDAY'
        WHEN 6 THEN 'SATURDAY'
        WHEN 7 THEN 'SUNDAY'
    END,
    base.scheduled_start::time,
    EXTRACT(
        EPOCH FROM (base.scheduled_end - base.scheduled_start)
    )::integer / 60,
    stats.starts_on,
    stats.ends_on,
    stats.total_occurrences,
    CASE
        WHEN stats.active_count > 0 THEN 'ACTIVE'
        WHEN stats.completed_count > 0 THEN 'COMPLETED'
        ELSE 'CANCELLED'
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM legacy_base base
JOIN legacy_stats stats
    ON stats.recurring_group_id = base.recurring_group_id
ON CONFLICT (id) DO NOTHING;

UPDATE appointments
SET
    appointment_series_id = recurring_group_id
WHERE recurring_group_id IS NOT NULL
  AND appointment_series_id IS NULL;

WITH numbered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY appointment_series_id
            ORDER BY scheduled_start, id
        )::integer AS occurrence_number
    FROM appointments
    WHERE appointment_series_id IS NOT NULL
)
UPDATE appointments
SET occurrence_number = numbered.occurrence_number
FROM numbered
WHERE appointments.id = numbered.id
  AND appointments.occurrence_number IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM appointments
        WHERE recurring_group_id IS NOT NULL
          AND appointment_series_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'Existem appointments recorrentes sem série associada';
    END IF;
END
$$;

SELECT COUNT(*)
FROM appointments
WHERE appointment_series_id IS NOT NULL
  AND occurrence_number IS NULL;
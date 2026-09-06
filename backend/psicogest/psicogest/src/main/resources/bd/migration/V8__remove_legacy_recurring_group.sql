DROP INDEX IF EXISTS
    idx_appointments_recurring_group;


ALTER TABLE appointments
    DROP COLUMN recurring_group_id;
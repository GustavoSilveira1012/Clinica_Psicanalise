-- PostgreSQL enum changes become usable after this migration commits.
ALTER TYPE appointment_status ADD VALUE IF NOT EXISTS 'RESCHEDULED';

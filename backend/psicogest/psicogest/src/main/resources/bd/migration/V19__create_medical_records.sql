-- Compatibility marker. The initial schema already creates a legacy table;
-- V20 is the canonical encrypted medical-record definition and recreates it
-- in a controlled migration before dependent records are introduced.
SELECT 1;

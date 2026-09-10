-- Adiciona clinic_id ao Payment para auditoria e rastreabilidade
ALTER TABLE payments
ADD COLUMN clinic_id BIGINT;

ALTER TABLE payments
ADD CONSTRAINT fk_payment_clinic
FOREIGN KEY (clinic_id)
REFERENCES clinics(id)
ON DELETE RESTRICT;

CREATE INDEX idx_payment_clinic
ON payments(clinic_id, created_at DESC);

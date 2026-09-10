ALTER TABLE payments
ADD COLUMN request_fingerprint VARCHAR(64);

CREATE INDEX idx_payment_request_fingerprint
ON payments(request_fingerprint);
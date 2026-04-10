INSERT INTO payment_attempts
(id, payment_id, provider, status, error_message, attempt_number, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?);
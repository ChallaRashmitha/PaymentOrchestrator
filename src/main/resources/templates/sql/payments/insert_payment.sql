INSERT INTO payments
(id, idempotency_key, amount, currency, method, status, from_account_id, to_account_id, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
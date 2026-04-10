UPDATE payments
SET status = ?, provider = ?, transaction_id = ?, updated_at = ?
WHERE id = ?;
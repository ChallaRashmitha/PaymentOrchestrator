SELECT *
FROM payment_attempts
WHERE payment_id = ?
ORDER BY attempt_number;
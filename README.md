# Payment Orchestrator

A Spring Boot payment orchestration service that creates payments, applies idempotency checks, debits and credits accounts, and routes payment execution through provider implementations.

## Tech Stack

- Java 17
- Spring Boot 4.0.5
- Spring Web MVC
- Spring JDBC
- PostgreSQL
- Lombok
- JUnit 5 and Mockito

## Project Structure

```text
src/main/java/com/yuno/payment
|-- controller     REST API layer
|-- service        Payment and account business logic
|-- repository     Repository contracts and JDBC implementations
|-- dao            Thin JdbcTemplate read/write wrappers
|-- model          Domain models
|-- dto            Request and response DTOs
|-- mapper         DTO/domain mapping
|-- provider       Payment provider implementations
|-- factory        Provider selection by payment method
|-- executor       Provider execution flow
|-- util           SQL file loading
```

SQL templates are stored under:

```text
src/main/resources/templates/sql
|-- account
|-- idempotency
|-- payments
```

## Payment Flow

1. A client sends a create-payment request with an idempotency key.
2. The service checks whether the idempotency key already exists.
3. If the key exists, the existing payment response is returned.
4. If the key is new, the sender account is debited.
5. A payment row is created with `CREATED` status.
6. The payment moves to `PROCESSING`.
7. Providers are selected based on the payment method.
8. If provider execution succeeds, the receiver account is credited and the payment becomes `SUCCESS`.
9. If provider execution fails, the sender is refunded and the payment becomes `FAILED`.
10. The idempotency record is saved for future duplicate requests.

## API

### Create Payment

```http
POST /payments
Content-Type: application/json
```

Request body:

```json
{
  "amount": 100,
  "currency": "INR",
  "method": "CARD",
  "fromAccountId": "00000000-0000-0000-0000-000000000001",
  "toAccountId": "00000000-0000-0000-0000-000000000002",
  "idempotencyKey": "manual-test-123"
}
```

Response body:

```json
{
  "paymentId": "generated-payment-id",
  "status": "SUCCESS",
  "transactionId": "TXN_A_..."
}
```

Supported payment methods:

- `CARD`
- `UPI`

### Get Payment

```http
GET /payments/{id}
```

Response body:

```json
{
  "paymentId": "payment-id",
  "amount": 100,
  "currency": "INR",
  "method": "CARD",
  "status": "SUCCESS",
  "provider": "PROVIDER_A",
  "transactionId": "TXN_A_..."
}
```

## Database

The application expects PostgreSQL to be available at the datasource configured in:

```text
src/main/resources/application.yml
```

Current default database:

```text
jdbc:postgresql://localhost:5432/payment_db
```

Minimum expected tables:

```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    balance BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(50),
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    transaction_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE idempotency (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    payment_id UUID NOT NULL,
    created_at TIMESTAMP
);
```

Example seed data:

```sql
INSERT INTO accounts (id, user_id, balance, currency, created_at)
VALUES
('00000000-0000-0000-0000-000000000001', 'sender', 10000, 'INR', NOW()),
('00000000-0000-0000-0000-000000000002', 'receiver', 1000, 'INR', NOW());
```

## Run Locally

Start PostgreSQL and make sure `payment_db` exists.

Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

The server starts on:

```text
http://localhost:8080
```

## Test

Run all tests:

```powershell
.\mvnw.cmd test
```

Run compile only:

```powershell
.\mvnw.cmd compile
```

## Notes

- Do not reuse an idempotency key for a different payment request. The service treats duplicate keys as replay requests and returns the original payment.
- Keep database passwords out of committed configuration for real projects. Prefer environment variables or local-only profile files.
- The current tests are mostly unit tests and do not require PostgreSQL to be running.

# Payment Orchestrator - Architecture & Low Level Design (LLD)

## Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Workflow](#workflow)
4. [Low Level Design (LLD)](#low-level-design-lld)
5. [Database Schema](#database-schema)
6. [API Documentation](#api-documentation)
7. [Error Handling](#error-handling)
8. [Code Coverage](#code-coverage)

---

## Overview

**Payment Orchestrator** is a Spring Boot microservice that orchestrates payment processing across multiple payment providers. It provides a unified interface for creating payments, retrieving payment details, managing accounts, and tracking payment attempts.

### Key Features:
- ✅ Create payments with idempotency support
- ✅ Retrieve complete payment details with attempt history
- ✅ Multi-provider payment support (PROVIDER_A, PROVIDER_B)
- ✅ Account management (balance tracking, fund transfers)
- ✅ Payment status tracking (CREATED, PROCESSING, SUCCESS, FAILED)
- ✅ Retry mechanism with multiple payment providers
- ✅ Comprehensive audit trail via payment attempts
- ✅ JDBC-based persistence layer
- ✅ Exception handling with custom error responses

---

## System Architecture

### High-Level Architecture Diagram:

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Clients                              │
│                   (Web/Mobile/Third-party)                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REST Controllers                              │
│              (PaymentController)                                 │
│   - POST /payments (Create Payment)                              │
│   - GET /payments/{id} (Get Payment Details)                     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Service Layer                                 │
│   - PaymentService (Business Logic)                              │
│   - AccountService (Account Management)                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
┌──────────────┐  ┌────────────────┐  ┌────────────────┐
│  Executor    │  │  Repository    │  │  Factory       │
│  Layer       │  │  Layer         │  │  Layer         │
│              │  │                │  │                │
│ - Payment    │  │ - Payment      │  │ - Provider     │
│   Executor   │  │ - Account      │  │   Factory      │
│ - Provider   │  │ - Idempotency  │  │                │
│   Manager    │  │ - Attempt      │  │                │
└──────────────┘  └────────────────┘  └────────────────┘
        │                │                ▼
        │                │          ┌────────────────┐
        │                │          │  Providers     │
        │                │          │                │
        │                │          │ - Provider A   │
        │                │          │ - Provider B   │
        │                │          └────────────────┘
        └────────────────┼────────────────┘
                         ▼
        ┌──────────────────────────────────┐
        │  Data Access Layer (DAO/JDBC)    │
        │  - ReadDao (Queries)             │
        │  - WriteDao (Updates)            │
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │    PostgreSQL Database           │
        │  - Payments Table                │
        │  - Accounts Table                │
        │  - Payment Attempts Table        │
        │  - Idempotency Keys Table        │
        └──────────────────────────────────┘
```

### Component Breakdown:

| Component | Responsibility |
|-----------|-----------------|
| **PaymentController** | HTTP endpoint handling, request validation, response mapping |
| **PaymentService** | Business logic for payment creation, orchestration, status management |
| **AccountService** | Account balance management, fund transfers |
| **PaymentExecutor** | Core payment execution logic, provider selection, retry mechanism |
| **ProviderFactory** | Factory pattern for instantiating payment providers |
| **PaymentRepository** | Payment CRUD operations and queries |
| **AccountRepository** | Account CRUD operations |
| **PaymentAttemptRepository** | Payment attempt logging and retrieval |
| **IdempotencyRepository** | Idempotency key tracking |
| **ReadDao / WriteDao** | Low-level JDBC operations (Query & Update) |

---

## Workflow

### 1. Payment Creation Workflow

```
Client Request (POST /payments)
        │
        ▼
PaymentController.createPayment()
        │
        ├─ Validate Request
        │
        ├─ Log Request Details
        │
        ▼
PaymentService.createPayment()
        │
        ├─ Check Idempotency (prevent duplicate payments)
        │   └─ If exists, return cached response
        │
        ├─ Create Payment Entity (Status: CREATED)
        │
        ├─ Save Payment to Database
        │
        ├─ Execute Payment (PaymentExecutor)
        │   │
        │   ├─ Deduct Amount from Sender Account
        │   │
        │   ├─ Select Provider (via ProviderFactory)
        │   │
        │   ├─ Attempt Payment with Selected Provider
        │   │   └─ Log Payment Attempt
        │   │
        │   ├─ If Failed & Retries Available:
        │   │   └─ Try Next Provider
        │   │
        │   └─ Update Payment Status (SUCCESS/FAILED)
        │
        ├─ If Successful: Credit Amount to Receiver Account
        │
        ├─ Store Idempotency Key for Future Requests
        │
        ▼
Return PaymentResponse (Status 201 CREATED)
```

### 2. Payment Retrieval Workflow

```
Client Request (GET /payments/{id})
        │
        ▼
PaymentController.getPayment()
        │
        ├─ Validate Payment ID
        │
        ├─ Log Request Details
        │
        ▼
PaymentService.getPayment()
        │
        ├─ Query Payment from Database
        │
        ├─ Query Payment Attempts from Database
        │
        ├─ Populate PaymentDetailsResponse
        │   ├─ Payment Details
        │   ├─ Account Information
        │   └─ Payment Attempts List
        │
        ▼
Return PaymentDetailsResponse (Status 200 OK)
```

### 3. Payment Execution with Retry Mechanism

```
Execute Payment
        │
        ▼
Loop through available providers:
        │
        ├─ Provider A
        │   ├─ Send Payment Request
        │   │
        │   ├─ Receive Response
        │   │
        │   ├─ Log Attempt (Success/Failure)
        │   │
        │   └─ If Success:
        │       └─ Update Payment Status → SUCCESS
        │       └─ Exit Loop
        │   │
        │   └─ If Failure:
        │       └─ Try Next Provider
        │
        ├─ Provider B (if Provider A failed)
        │   ├─ Send Payment Request
        │   │
        │   ├─ Receive Response
        │   │
        │   ├─ Log Attempt (Success/Failure)
        │   │
        │   └─ If Success:
        │       └─ Update Payment Status → SUCCESS
        │       └─ Exit Loop
        │   │
        │   └─ If Failure:
        │       └─ Update Payment Status → FAILED
        │       └─ Exit Loop
        │
        ▼
Return Final Status
```

---

## Low Level Design (LLD)

### 1. Entity Models

#### Payment Entity
```java
class Payment {
    UUID id;                    // Unique Payment Identifier
    String idempotencyKey;      // For idempotent requests
    Long amount;                // Payment Amount
    String currency;            // Currency Code (INR, USD, EUR)
    PaymentMethod method;       // Payment Method (CARD, UPI, WALLET)
    PaymentStatus status;       // Current Status (CREATED, PROCESSING, SUCCESS, FAILED)
    UUID fromAccountId;         // Sender Account
    UUID toAccountId;           // Receiver Account
    String provider;            // Selected Provider
    String transactionId;       // External Transaction ID
    Timestamp createdAt;        // Creation Time
    Timestamp updatedAt;        // Last Update Time
}
```

#### PaymentAttempt Entity
```java
class PaymentAttempt {
    UUID id;                    // Unique Attempt Identifier
    UUID paymentId;             // Reference to Payment
    Provider provider;          // Provider Used
    PaymentStatus status;       // Attempt Status
    String errorMessage;        // Error Message (if failed)
    int attemptNumber;          // Retry Attempt Number
    Timestamp createdAt;        // Attempt Timestamp
}
```

#### Account Entity
```java
class Account {
    UUID id;                    // Unique Account Identifier
    String accountNumber;       // Account Number
    String accountHolder;       // Account Holder Name
    Long balance;               // Current Balance
    String currency;            // Currency
    Timestamp createdAt;        // Creation Time
    Timestamp updatedAt;        // Last Update Time
}
```

#### Idempotency Key Entity
```java
class IdempotencyKey {
    UUID id;                    // Unique Key Identifier
    String key;                 // Idempotency Key String
    UUID paymentId;             // Associated Payment
    Timestamp createdAt;        // Key Creation Time
    Timestamp expiresAt;        // Key Expiration Time (24 hours)
}
```

### 2. Service Layer Design

#### PaymentService
**Responsibilities:**
- Create new payments
- Retrieve payment details
- Validate payment requests
- Coordinate with repositories and executors

**Key Methods:**
```java
PaymentResponse createPayment(CreatePaymentRequest request)
PaymentDetailsResponse getPayment(UUID paymentId)
```

#### AccountService
**Responsibilities:**
- Account balance management
- Fund transfer between accounts
- Account lookup

**Key Methods:**
```java
Account getAccount(UUID accountId)
void deductBalance(UUID accountId, Long amount)
void creditBalance(UUID accountId, Long amount)
```

### 3. Repository Layer Design

#### Pattern Used: Repository Pattern with Data Access Objects (DAO)

```
┌─────────────────────────────────────────┐
│        Repository Interface             │
├─────────────────────────────────────────┤
│ + save(entity)                          │
│ + findById(id)                          │
│ + findAll()                             │
│ + update(entity)                        │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│   Repository Implementation             │
│   (PaymentRepositoryImpl, etc.)          │
├─────────────────────────────────────────┤
│ - sqlLoader (load SQL from files)       │
│ - readDao (execute SELECT queries)      │
│ - writeDao (execute INSERT/UPDATE)      │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│     Data Access Objects (DAO)           │
│   (ReadDao, WriteDao)                   │
├─────────────────────────────────────────┤
│ - Query via RowMapper                   │
│ - PreparedStatement execution           │
│ - JDBC Template integration             │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│      JDBC & SQL Execution               │
├─────────────────────────────────────────┤
│ - JdbcTemplate                          │
│ - ResultSet mapping                     │
│ - Parameter binding                     │
└─────────────────────────────────────────┘
```

### 4. Executor Layer Design

#### PaymentExecutor
**Responsibilities:**
- Orchestrate payment execution
- Manage provider selection and retry logic
- Track payment attempts

**Pseudo-code:**
```java
public PaymentResponse executePayment(Payment payment) {
    List<Provider> providers = availableProviders;
    
    for (int attempt = 0; attempt < providers.size(); attempt++) {
        Provider provider = providers.get(attempt);
        
        try {
            PaymentResult result = provider.processPayment(payment);
            
            logAttempt(payment.getId(), provider, SUCCESSFUL);
            
            if (result.isSuccessful()) {
                updatePaymentStatus(payment.getId(), SUCCESS);
                return success;
            }
        } catch (Exception e) {
            logAttempt(payment.getId(), provider, FAILED, e.getMessage());
            
            if (attempt == providers.size() - 1) {
                updatePaymentStatus(payment.getId(), FAILED);
                throw e;
            }
        }
    }
}
```

### 5. Factory Pattern

#### ProviderFactory
**Responsibilities:**
- Create appropriate payment provider instances
- Manage provider lifecycle

**Design:**
```
ProviderFactory
├─ getProvider(Provider type)
│  ├─ PROVIDER_A → ProviderA instance
│  ├─ PROVIDER_B → ProviderB instance
│  └─ Default → Exception
│
└─ getAvailableProviders()
   └─ List of all registered providers
```

### 6. Exception Handling Strategy

```
Exception Hierarchy:
│
├─ PaymentExecutionException
│  └─ When payment execution fails
│
├─ PaymentNotFoundException
│  └─ When payment ID not found
│
├─ InvalidPaymentAmountException
│  └─ When amount is invalid
│
├─ InvalidPaymentStateException
│  └─ When payment state transition is invalid
│
├─ InsufficientBalanceException
│  └─ When account has insufficient balance
│
└─ SqlLoadException
   └─ When SQL file loading fails

GlobalExceptionHandler
├─ Catches all exceptions
├─ Logs error details
├─ Returns appropriate HTTP status
└─ Provides user-friendly error messages
```

---

## Database Schema

### Tables Overview

#### 1. payments table
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE,
    amount BIGINT NOT NULL,
    currency VARCHAR(10),
    method VARCHAR(50),
    status VARCHAR(50),
    from_account_id UUID,
    to_account_id UUID,
    provider VARCHAR(50),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 2. payment_attempts table
```sql
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID REFERENCES payments(id),
    provider VARCHAR(50),
    status VARCHAR(50),
    error_message TEXT,
    attempt_number INT,
    created_at TIMESTAMP
);
```

#### 3. accounts table
```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50),
    account_holder VARCHAR(255),
    balance BIGINT,
    currency VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 4. idempotency_keys table
```sql
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    key VARCHAR(255) UNIQUE,
    payment_id UUID REFERENCES payments(id),
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);
```

---

## API Documentation

### 1. Create Payment API

**Endpoint:** `POST /payments`

**Request Body:**
```json
{
    "amount": 1000,
    "currency": "INR",
    "method": "CARD",
    "fromAccountId": "11111111-1111-1111-1111-111111111111",
    "toAccountId": "22222222-2222-2222-2222-222222222222",
    "idempotencyKey": "yuno-test-123"
}
```

**Response (201 CREATED):**
```json
{
    "paymentId": "de68f597-d651-48ba-a1fd-1cfeb3444567",
    "status": "SUCCESS",
    "transactionId": "txn-1"
}
```

### 2. Get Payment Details API

**Endpoint:** `GET /payments/{id}`

**Path Parameters:**
- `id` (UUID): Payment ID

**Response (200 OK):**
```json
{
    "paymentId": "de68f597-d651-48ba-a1fd-1cfeb3444567",
    "amount": 1000,
    "currency": "INR",
    "method": "CARD",
    "status": "SUCCESS",
    "provider": "PROVIDER_A",
    "transactionId": "txn-1",
    "attempts": [
        {
            "provider": "PROVIDER_A",
            "status": "SUCCESS",
            "errorMessage": null,
            "attemptNumber": 1
        }
    ]
}
```

### 3. Payment Status Flow

```
CREATED
  │
  ▼
PROCESSING (during execution)
  │
  ├─ SUCCESS (payment processed successfully)
  │
  └─ FAILED (all retry attempts exhausted)
```

---

## Error Handling

### Error Response Format

```json
{
    "timestamp": "2024-04-10T10:30:00Z",
    "status": 400,
    "error": "Invalid Payment Amount",
    "message": "Payment amount must be greater than zero",
    "path": "/payments"
}
```

### Common Error Scenarios

| Scenario | Status | Error Code |
|----------|--------|-----------|
| Invalid amount | 400 | BAD_REQUEST |
| Payment not found | 404 | NOT_FOUND |
| Insufficient balance | 400 | BAD_REQUEST |
| Invalid payment state | 409 | CONFLICT |
| Database error | 500 | INTERNAL_SERVER_ERROR |

---

## Code Coverage

### Current Coverage Metrics

| Metric | Coverage | Target |
|--------|----------|--------|
| **Instruction Coverage** | 91% | 90% ✅ |
| **Branch Coverage** | 95% | 90% ✅ |
| **Line Coverage** | 93% | - ✅ |
| **Method Coverage** | 98% | - ✅ |

### Package-wise Coverage

| Package | Instruction | Branch | Status |
|---------|-------------|--------|--------|
| **com.yuno.payment.service** | 100% | 100% | ✅ |
| **com.yuno.payment.executor** | 100% | 100% | ✅ |
| **com.yuno.payment.controller** | 100% | n/a | ✅ |
| **com.yuno.payment.factory** | 100% | 100% | ✅ |
| **com.yuno.payment.model** | 100% | 93% | ✅ |
| **com.yuno.payment.dao** | 90% | 50% | ⚠️ |
| **com.yuno.payment.mapper** | 84% | 100% | ⚠️ |
| **com.yuno.payment.repository** | 75% | 100% | ⚠️ |

---

## Testing Strategy

### Test Levels

1. **Unit Tests**
   - Service layer tests
   - Repository tests
   - Executor tests
   - Controller tests

2. **Integration Tests**
   - Database integration
   - End-to-end workflow

3. **Test Coverage Areas**
   - Happy path scenarios
   - Error scenarios
   - Retry mechanisms
   - Exception handling
   - Edge cases (null handling, empty lists, etc.)

### Test Execution

```bash
# Run all tests with coverage
mvn clean test

# Run specific test class
mvn test -Dtest=PaymentControllerTest

# Run with coverage report
mvn clean verify
```

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 4.0.5 |
| **Language** | Java 17 |
| **Database** | PostgreSQL |
| **Build Tool** | Maven |
| **Testing** | JUnit 5, Mockito, AssertJ |
| **Code Coverage** | JaCoCo 0.8.13 |
| **Logging** | SLF4J + Logback |
| **Data Access** | JDBC, JdbcTemplate |

---

## Development Practices

### Code Quality Standards
- ✅ Minimum 90% code coverage
- ✅ All public methods documented
- ✅ Custom exceptions for all error scenarios
- ✅ Comprehensive logging at INFO and ERROR levels
- ✅ Input validation on all API endpoints
- ✅ Idempotency support for payment creation

### Design Patterns Used
1. **Repository Pattern** - Data access abstraction
2. **Factory Pattern** - Provider instantiation
3. **Executor Pattern** - Payment execution orchestration
4. **Decorator Pattern** - Service composition
5. **Global Exception Handler** - Centralized error handling

---

## Future Enhancements

- [ ] Payment webhook notifications
- [ ] Real-time payment status updates (WebSocket)
- [ ] Payment scheduling/recurring payments
- [ ] Advanced fraud detection
- [ ] Payment analytics and reporting
- [ ] Rate limiting per account
- [ ] Multi-currency conversion
- [ ] Payment reversals/refunds

---

**Document Version:** 1.0  
**Last Updated:** April 10, 2026  
**Author:** Payment Orchestrator Team

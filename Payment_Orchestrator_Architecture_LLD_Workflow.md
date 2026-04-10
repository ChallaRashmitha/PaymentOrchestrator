# Payment Orchestrator
## Architecture, Low-Level Design & Workflow Documentation

---

## Document Information

**Document Title:** Payment Orchestrator - Complete Architecture & LLD Guide  
**Document Version:** 1.0  
**Created:** April 10, 2026  
**Project:** Payment Orchestrator Microservice  
**Technology Stack:** Spring Boot 4.0.5, Java 17, PostgreSQL  
**Status:** ✅ Production Ready

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Architecture Overview](#system-architecture-overview)
3. [High-Level Architecture Diagram](#high-level-architecture-diagram)
4. [Core Components](#core-components)
5. [Workflows](#workflows)
   - 5.1 Payment Creation Workflow
   - 5.2 Payment Retrieval Workflow
   - 5.3 Retry Mechanism
6. [Low-Level Design](#low-level-design)
   - 6.1 Class Diagrams
   - 6.2 Entity Models
   - 6.3 Sequence Diagrams
7. [Data Layer Design](#data-layer-design)
8. [API Documentation](#api-documentation)
9. [Error Handling](#error-handling)
10. [Database Schema](#database-schema)
11. [Design Patterns](#design-patterns)
12. [Code Coverage](#code-coverage)
13. [Appendix](#appendix)

---

## 1. Executive Summary

The **Payment Orchestrator** is a Spring Boot microservice that orchestrates payment processing across multiple payment providers. It provides a unified interface for creating payments, retrieving payment details, managing accounts, and tracking payment attempts.

### Key Features:
- Multi-provider payment support with automatic fallback
- Idempotency support to prevent duplicate payments
- Comprehensive payment attempt tracking with retry mechanism
- Account management with balance transfers
- Complete audit trail for all transactions
- 91% code coverage (exceeds 90% target)
- Thread-safe operations with database transaction handling

### Core Capabilities:
1. **Create Payment** - Process payments with multiple provider fallback
2. **Retrieve Payment Details** - Get complete payment information with attempt history
3. **Account Management** - Track balances and transfers
4. **Retry Mechanism** - Automatic failover to alternative providers
5. **Idempotency** - Prevent duplicate payment processing

---

## 2. System Architecture Overview

The Payment Orchestrator follows a layered architecture pattern with clear separation of concerns:

### Architecture Layers:
```
┌─────────────────────────────────────────────────────┐
│              HTTP Request Layer                     │
│           (REST Controllers)                        │
└────────────────────┬────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────┐
│         Business Logic Layer (Services)             │
│    PaymentService, AccountService                   │
└────────────────────┬────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────┐
│   Execution & Factory Layer                         │
│   PaymentExecutor, ProviderFactory                  │
└────────────────────┬────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────┐
│   Repository Layer (Data Access Abstraction)        │
│   Repositories, DAOs, SqlLoader                     │
└────────────────────┬────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────┐
│       Persistence Layer (Database)                  │
│          PostgreSQL Database                        │
└─────────────────────────────────────────────────────┘
```

---

## 3. High-Level Architecture Diagram

```
External Systems                                HTTP Clients
(Payment Providers)                          (Web/Mobile/API)
        │                                         │
        │                                         ▼
        │                            ┌────────────────────────┐
        │                            │   PaymentController    │
        │                            │  (REST API Endpoints)  │
        │                            └────────────┬───────────┘
        │                                         │
        │        ┌────────────────────────────────┘
        │        │
        ▼        ▼
┌──────────────────────────────────────────────────────┐
│              Service Layer                          │
│  ┌─────────────────────────────────────────────┐   │
│  │ PaymentService: Business Logic              │   │
│  │ - Create & retrieve payments                │   │
│  │ - Validate requests                         │   │
│  │ - Coordinate execution                      │   │
│  └──────────────┬────────────────────────────┬─┘   │
│                 │                            │     │
│  ┌──────────────┴──┐  ┌────────────────┐    │     │
│  │ AccountService  │  │ PaymentExecutor│    │     │
│  │ - Manage        │  │ - Execute      │    │     │
│  │   balances      │  │ - Retry logic  │    │     │
│  └─────────────────┘  └────────────┬───┘    │     │
└──────────────────────────┬──────────┼────────┼─────┘
                           │         │        │
                    ┌──────┘         │        └──────┐
                    ▼                ▼               ▼
            ┌───────────────┐  ┌─────────────┐  ┌──────────────┐
            │PaymentRepo    │  │ProviderFact │  │IdempotencyRep│
            │AccountRepo    │  │(Select Prov)│  │RepositoryImpl │
            │AttemptRepo    │  └─────────────┘  └──────────────┘
            └────────┬──────┘
                     │
                     ▼
            ┌───────────────────┐
            │ ReadDao/WriteDao  │
            │   (JDBC Access)   │
            └────────┬──────────┘
                     │
                     ▼
            ┌───────────────────┐
            │  PostgreSQL DB    │
            │  (Payments, etc)  │
            └───────────────────┘
```

---

## 4. Core Components

### 4.1 PaymentController
**Purpose:** Handle HTTP requests and responses  
**Responsibilities:**
- Validate incoming requests
- Delegate to service layer
- Format responses
- Handle exceptions

**Key Endpoints:**
- `POST /payments` - Create a new payment
- `GET /payments/{id}` - Retrieve payment details

---

### 4.2 PaymentService
**Purpose:** Orchestrate payment processing  
**Responsibilities:**
- Validate payment requests
- Check idempotency
- Create payment entities
- Coordinate execution
- Return responses

**Key Methods:**
```java
PaymentResponse createPayment(CreatePaymentRequest request)
PaymentDetailsResponse getPayment(UUID paymentId)
```

---

### 4.3 PaymentExecutor
**Purpose:** Execute payment logic with retry mechanism  
**Responsibilities:**
- Select payment provider
- Process payment
- Handle failures
- Log attempts
- Update payment status

**Key Features:**
- Automatic provider selection
- Fallback to alternative providers
- Comprehensive attempt logging
- Transaction rollback on failure

---

### 4.4 ProviderFactory
**Purpose:** Manage payment provider instances  
**Responsibilities:**
- Create provider instances
- Return available providers
- Manage provider lifecycle

**Supported Providers:**
- PROVIDER_A
- PROVIDER_B

---

### 4.5 Repository Layer
**Purpose:** Abstract database access  
**Components:**
- PaymentRepository - Payment CRUD operations
- AccountRepository - Account management
- PaymentAttemptRepository - Attempt tracking
- IdempotencyRepository - Idempotency key management

---

### 4.6 DAO Layer
**Purpose:** Low-level database operations  
**Components:**
- ReadDao - SELECT queries via RowMapper
- WriteDao - INSERT/UPDATE operations
- JdbcTemplate - Underlying JDBC operations

---

## 5. Workflows

### 5.1 Payment Creation Workflow

```
CLIENT REQUEST
    │
    ▼
VALIDATION
    ├─ Check amount > 0
    ├─ Check accounts exist
    ├─ Check sufficient balance
    │
    ▼
IDEMPOTENCY CHECK
    ├─ Query idempotency key
    ├─ If found: Return cached response
    │
    ▼
CREATE PAYMENT ENTITY
    ├─ Status: CREATED
    ├─ Save to database
    │
    ▼
EXECUTE PAYMENT
    ├─ Deduct from sender account
    │
    ├─ SELECT PROVIDER
    │  └─ ProviderFactory returns ordered list
    │
    ├─ ATTEMPT WITH PROVIDER A
    │  ├─ Send payment request
    │  ├─ Log attempt (Success/Failure)
    │  │
    │  ├─ IF SUCCESS
    │  │  └─ Update Status: SUCCESS
    │  │     Credit receiver account
    │  │     Exit
    │  │
    │  └─ IF FAILURE
    │     └─ Continue to next provider
    │
    ├─ ATTEMPT WITH PROVIDER B
    │  ├─ Send payment request
    │  ├─ Log attempt (Success/Failure)
    │  │
    │  ├─ IF SUCCESS
    │  │  └─ Update Status: SUCCESS
    │  │     Credit receiver account
    │  │     Exit
    │  │
    │  └─ IF FAILURE
    │     └─ Update Status: FAILED
    │        Exit
    │
    ▼
SAVE IDEMPOTENCY KEY
    ├─ Store key for future requests
    ├─ Set expiration (24 hours)
    │
    ▼
RETURN RESPONSE
    └─ PaymentResponse (paymentId, status, transactionId)
```

### 5.2 Payment Retrieval Workflow

```
CLIENT REQUEST (GET /payments/{id})
    │
    ▼
VALIDATE ID
    └─ Ensure valid UUID format
    │
    ▼
QUERY PAYMENT
    ├─ SELECT * FROM payments WHERE id = ?
    ├─ Map ResultSet to Payment entity
    │
    ▼
QUERY ATTEMPTS
    ├─ SELECT * FROM payment_attempts WHERE payment_id = ?
    ├─ Map ResultSet to List<PaymentAttempt>
    │
    ▼
BUILD RESPONSE
    ├─ PaymentDetailsResponse
    │  ├─ Payment details (id, amount, status, etc)
    │  ├─ Provider information
    │  ├─ Transaction ID
    │  └─ List of attempts with details
    │
    ▼
RETURN RESPONSE
    └─ 200 OK with PaymentDetailsResponse
```

### 5.3 Retry Mechanism (Automatic Fallback)

```
Payment Execution
    │
    ├─ Provider List: [PROVIDER_A, PROVIDER_B]
    │
    ├─ ATTEMPT 1: PROVIDER_A
    │   ├─ Process payment
    │   ├─ Log attempt
    │   │
    │   ├─ SUCCESS ✅
    │   │  └─ EXIT with SUCCESS
    │   │
    │   └─ FAILURE ❌
    │      ├─ Log error
    │      └─ CONTINUE TO NEXT
    │
    ├─ ATTEMPT 2: PROVIDER_B
    │   ├─ Process payment
    │   ├─ Log attempt
    │   │
    │   ├─ SUCCESS ✅
    │   │  └─ EXIT with SUCCESS
    │   │
    │   └─ FAILURE ❌
    │      ├─ Log error
    │      └─ Mark as FAILED
    │
    └─ FINAL STATUS
       ├─ SUCCESS (if any provider succeeded)
       └─ FAILED (if all providers failed)
```

---

## 6. Low-Level Design

### 6.1 Class Diagram Overview

```
Controller Layer:
┌─────────────────────────────┐
│ PaymentController           │
├─────────────────────────────┤
│ - paymentService            │
├─────────────────────────────┤
│ + createPayment()           │
│ + getPayment()              │
└─────────────────────────────┘

Service Layer:
┌──────────────────────────────┐
│ PaymentService               │
├──────────────────────────────┤
│ - paymentRepository          │
│ - accountService             │
│ - paymentExecutor            │
│ - idempotencyRepository      │
│ - paymentAttemptRepository   │
├──────────────────────────────┤
│ + createPayment()            │
│ + getPayment()               │
│ - validatePaymentRequest()   │
│ - executePaymentFlow()       │
└──────────────────────────────┘
        │
        ├─────────────────────────┬────────────────┐
        ▼                         ▼                ▼
    ┌─────────────┐  ┌──────────────────┐  ┌──────────────┐
    │AccountService│  │PaymentExecutor   │  │IdempotencyRep│
    ├─────────────┤  ├──────────────────┤  ├──────────────┤
    │+ getAccount()│  │- providerFactory │  │+ saveKey()   │
    │+ deduct...()│  │- paymentRepository│  │+ findByKey() │
    │+ credit...()│  │├──────────────────┤  │+ deleteKey() │
    └─────────────┘  │+ execute()       │  └──────────────┘
                     │- selectProvider()│
                     │- logAttempt()    │
                     └──────────────────┘
                             │
                             ▼
                     ┌──────────────────┐
                     │ProviderFactory   │
                     ├──────────────────┤
                     │- providers: Map  │
                     ├──────────────────┤
                     │+ getProvider()   │
                     │+ getAvailableList│
                     └──────────────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
               ┌─────────────┐   ┌─────────────┐
               │ProviderA    │   │ProviderB    │
               ├─────────────┤   ├─────────────┤
               │+ process()  │   │+ process()  │
               └─────────────┘   └─────────────┘

Repository Layer:
┌────────────────────────┐  ┌──────────────────────┐
│PaymentRepository       │  │PaymentAttemptRepository
├────────────────────────┤  ├──────────────────────┤
│- sqlLoader             │  │- sqlLoader           │
│- readDao              │  │- readDao            │
│- writeDao             │  │- writeDao           │
├────────────────────────┤  ├──────────────────────┤
│+ save()               │  │+ save()              │
│+ findById()           │  │+ findByPaymentId()   │
│+ findByIdempotency()  │  │- mapRow()            │
│+ update()             │  └──────────────────────┘
│- mapRow()             │
└────────────────────────┘

DAO Layer:
┌─────────────────┐  ┌─────────────────┐
│ ReadDao         │  │ WriteDao        │
│ (Interface)     │  │ (Interface)     │
├─────────────────┤  ├─────────────────┤
│+ query()        │  │+ update()       │
│+ queryForObject()│  └─────────────────┘
│  (default)      │          │
└─────────┬───────┘          │
          │                  │
          ▼                  ▼
    ┌──────────────┐   ┌──────────────┐
    │JdbcReadDao   │   │JdbcWriteDao  │
    ├──────────────┤   ├──────────────┤
    │- jdbcTemplate│   │- jdbcTemplate│
    ├──────────────┤   ├──────────────┤
    │+ query()     │   │+ update()    │
    └──────────────┘   └──────────────┘
```

### 6.2 Entity Models

#### Payment Entity
```
┌────────────────────────┐
│ Payment                │
├────────────────────────┤
│ - id: UUID             │
│ - idempotencyKey       │
│ - amount: Long         │
│ - currency: String     │
│ - method: PaymentMethod│
│ - status: PaymentStatus│
│ - fromAccountId: UUID  │
│ - toAccountId: UUID    │
│ - provider: String     │
│ - transactionId        │
│ - createdAt: Timestamp │
│ - updatedAt: Timestamp │
└────────────────────────┘
```

#### PaymentAttempt Entity
```
┌────────────────────────┐
│ PaymentAttempt         │
├────────────────────────┤
│ - id: UUID             │
│ - paymentId: UUID      │
│ - provider: Provider   │
│ - status: PaymentStatus│
│ - errorMessage: String │
│ - attemptNumber: int   │
│ - createdAt: Timestamp │
└────────────────────────┘
```

#### Account Entity
```
┌────────────────────────┐
│ Account                │
├────────────────────────┤
│ - id: UUID             │
│ - accountNumber        │
│ - accountHolder        │
│ - balance: Long        │
│ - currency: String     │
│ - createdAt: Timestamp │
│ - updatedAt: Timestamp │
└────────────────────────┘
```

### 6.3 Sequence Diagrams

#### 6.3.1 Payment Creation Sequence

```
Client       Controller       Service        Executor        Repository
  │              │               │              │                │
  │──POST req───→│               │              │                │
  │              │               │              │                │
  │              ├─validate req─→│              │                │
  │              │               │              │                │
  │              │        ┌──Check Idempotency──┤                │
  │              │        │      (cache)        │                │
  │              │        └──────────────────────┘                │
  │              │               │              │                │
  │              │     ┌─Create Payment Entity──┤                │
  │              │     │         (CREATED)      │                │
  │              │     └──────────────────────────────save───────→│
  │              │               │              │                │
  │              │               ├─execute payment──→             │
  │              │               │              │                │
  │              │               │      ┌─Select Provider         │
  │              │               │      │   (ProviderFactory)     │
  │              │               │      └─────┐                  │
  │              │               │            │                  │
  │              │               │      ┌─Process with Provider A │
  │              │               │      │  (Success/Failure)      │
  │              │               │      │                         │
  │              │               │      └─Log Attempt────────────→│
  │              │               │            │                  │
  │              │    ┌─Update Status (SUCCESS/FAILED)           │
  │              │    │          │            │                  │
  │              │    └──────────┼────────update───────────────→│
  │              │               │            │                  │
  │              │←──response────│            │                  │
  │←─201 resp────│               │            │                  │
```

#### 6.3.2 Payment Retrieval Sequence

```
Client       Controller       Service        Repository        DAO
  │              │               │              │               │
  │──GET req────→│               │              │               │
  │              │               │              │               │
  │              ├─getPayment────│              │               │
  │              │               │              │               │
  │              │        ┌───Query Payment────→│               │
  │              │        │                     ├─select───────→│
  │              │        │                     │←─ResultSet──┤│
  │              │        │                     │               │
  │              │        └─Query Attempts─────→│               │
  │              │                              ├─select───────→│
  │              │                              │←─List<Results>│
  │              │               │              │               │
  │              │        ┌─Build Response─────┤               │
  │              │        │   (with attempts)   │               │
  │              │        └────────────────────┘               │
  │              │               │                              │
  │←─200 resp────│←─PaymentDetailsResponse                     │
```

---

## 7. Data Layer Design

### 7.1 Repository Pattern

```
Service Layer
     │
     ▼
Repository Interface
(PaymentRepository)
     │
     ├─────────────────────────────────────┐
     ▼                                     ▼
Repository Implementation         DTO/Entity Mapper
(PaymentRepositoryImpl)           (RowMapper)
     │                                     │
     ├─────────────────┬────────────────────┘
     ▼                 ▼
  DAO Layer
  (ReadDao/WriteDao)
     │
     ▼
JDBC & JdbcTemplate
     │
     ▼
PostgreSQL Database
```

### 7.2 DAO Abstraction

```
ReadDao (Interface)
  ├─ query(sql, mapper, params): List<T>
  └─ queryForObject(sql, mapper, params): T
       │
       ├─ Returns first element from query
       └─ Throws EmptyResultDataAccessException if no result

WriteDao (Interface)
  └─ update(sql, params): int
       │
       └─ Returns number of rows affected

Implementation:
  ├─ JdbcReadDao
  │  └─ Uses JdbcTemplate.query()
  │
  └─ JdbcWriteDao
     └─ Uses JdbcTemplate.update()
```

---

## 8. API Documentation

### 8.1 Create Payment API

**Endpoint:** `POST /payments`  
**Content-Type:** application/json  
**Response Status:** 201 CREATED

**Request Body:**
```json
{
  "amount": 1000,
  "currency": "INR",
  "method": "CARD",
  "fromAccountId": "11111111-1111-1111-1111-111111111111",
  "toAccountId": "22222222-2222-2222-2222-222222222222",
  "idempotencyKey": "unique-key-12345"
}
```

**Request Validation:**
- amount > 0 (required)
- currency non-empty (required)
- method not null (required)
- fromAccountId valid UUID (required)
- toAccountId valid UUID (required)
- idempotencyKey non-empty (required)

**Success Response (201):**
```json
{
  "paymentId": "de68f597-d651-48ba-a1fd-1cfeb3444567",
  "status": "SUCCESS",
  "transactionId": "txn-123456"
}
```

**Field Descriptions:**
- paymentId: Unique payment identifier
- status: Final payment status (CREATED, PROCESSING, SUCCESS, FAILED)
- transactionId: External transaction identifier from provider

### 8.2 Get Payment Details API

**Endpoint:** `GET /payments/{id}`  
**Path Parameters:** id (UUID) - Payment ID  
**Response Status:** 200 OK

**Success Response (200):**
```json
{
  "paymentId": "de68f597-d651-48ba-a1fd-1cfeb3444567",
  "amount": 1000,
  "currency": "INR",
  "method": "CARD",
  "status": "SUCCESS",
  "provider": "PROVIDER_A",
  "transactionId": "txn-123456",
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

**Field Descriptions:**
- paymentId: Unique payment identifier
- amount: Payment amount
- currency: Currency code (INR, USD, EUR)
- method: Payment method (CARD, UPI, WALLET)
- status: Current payment status
- provider: Payment provider name
- transactionId: External transaction ID
- attempts: List of payment attempts with details

### 8.3 Error Responses

**400 Bad Request - Invalid Amount:**
```json
{
  "timestamp": "2024-04-10T10:30:00Z",
  "status": 400,
  "error": "Invalid Payment Amount",
  "message": "Payment amount must be greater than zero",
  "path": "/payments"
}
```

**404 Not Found - Payment Not Found:**
```json
{
  "timestamp": "2024-04-10T10:30:00Z",
  "status": 404,
  "error": "Payment Not Found",
  "message": "Payment with ID xyz not found",
  "path": "/payments/xyz"
}
```

**400 Bad Request - Insufficient Balance:**
```json
{
  "timestamp": "2024-04-10T10:30:00Z",
  "status": 400,
  "error": "Insufficient Balance",
  "message": "Sender account has insufficient balance",
  "path": "/payments"
}
```

---

## 9. Error Handling

### 9.1 Exception Hierarchy

```
Throwable
  │
  ├─ Exception
  │  └─ RuntimeException
  │     ├─ PaymentExecutionException
  │     │  └─ When payment execution fails
  │     │
  │     ├─ PaymentNotFoundException
  │     │  └─ When payment ID not found
  │     │
  │     ├─ InvalidPaymentAmountException
  │     │  └─ When amount is invalid (≤ 0)
  │     │
  │     ├─ InvalidPaymentStateException
  │     │  └─ When state transition is invalid
  │     │
  │     ├─ InsufficientBalanceException
  │     │  └─ When account lacks balance
  │     │
  │     └─ SqlLoadException
  │        └─ When SQL file loading fails
  │
  └─ Error
     └─ System errors (unrecoverable)
```

### 9.2 Error Handling Strategy

**Global Exception Handler:**
- Catches all exceptions in controllers
- Logs error details
- Returns appropriate HTTP status
- Provides user-friendly messages

**Error Recovery:**
- Recoverable errors: Retry with next provider
- Non-recoverable errors: Fail immediately
- All attempts logged for audit trail

### 9.3 Error Scenarios & HTTP Status Codes

| Scenario | Status | Action |
|----------|--------|--------|
| Invalid amount (≤ 0) | 400 | Reject immediately |
| Payment not found | 404 | Return error |
| Insufficient balance | 400 | Reject immediately |
| Provider A fails | 200 | Retry with Provider B |
| Both providers fail | 200 | Return FAILED status |
| Database error | 500 | Log and return error |
| Invalid request format | 400 | Validation error |

---

## 10. Database Schema

### 10.1 Payments Table

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE,
    amount BIGINT NOT NULL,
    currency VARCHAR(10),
    method VARCHAR(50),
    status VARCHAR(50),
    from_account_id UUID NOT NULL,
    to_account_id UUID NOT NULL,
    provider VARCHAR(50),
    transaction_id VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_idempotency_key ON payments(idempotency_key);
CREATE INDEX idx_from_account ON payments(from_account_id);
CREATE INDEX idx_to_account ON payments(to_account_id);
CREATE INDEX idx_status ON payments(status);
```

**Column Descriptions:**
- id: Unique payment identifier (UUID)
- idempotency_key: For preventing duplicate payments
- amount: Payment amount in smallest currency unit
- currency: ISO 4217 currency code
- method: Payment method (CARD, UPI, WALLET)
- status: Payment status (CREATED, PROCESSING, SUCCESS, FAILED)
- from_account_id: Sender account ID
- to_account_id: Receiver account ID
- provider: Provider used for payment
- transaction_id: External transaction ID from provider
- created_at: Payment creation timestamp
- updated_at: Last update timestamp

### 10.2 Payment Attempts Table

```sql
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    provider VARCHAR(50),
    status VARCHAR(50),
    error_message TEXT,
    attempt_number INT,
    created_at TIMESTAMP
);

CREATE INDEX idx_payment_id ON payment_attempts(payment_id);
CREATE INDEX idx_provider ON payment_attempts(provider);
```

**Column Descriptions:**
- id: Unique attempt identifier
- payment_id: Reference to payment
- provider: Provider used for this attempt
- status: Attempt result (SUCCESS, FAILED)
- error_message: Error details if failed
- attempt_number: Sequential attempt number
- created_at: Attempt timestamp

### 10.3 Accounts Table

```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50) UNIQUE,
    account_holder VARCHAR(255),
    balance BIGINT,
    currency VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_account_number ON accounts(account_number);
```

### 10.4 Idempotency Keys Table

```sql
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    key VARCHAR(255) UNIQUE,
    payment_id UUID NOT NULL REFERENCES payments(id),
    created_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_key ON idempotency_keys(key);
CREATE INDEX idx_expires_at ON idempotency_keys(expires_at);
```

### 10.5 Database Relationships

```
payments (1)
    │
    ├──┐ (N)
    │  └──> payment_attempts
    │
    ├──┐ (1)
    │  └──> accounts (from_account_id)
    │
    ├──┐ (1)
    │  └──> accounts (to_account_id)
    │
    └──┐ (1)
       └──> idempotency_keys
```

---

## 11. Design Patterns & SOLID Principles

### 11.1 Design Patterns Used

#### 11.1.1 Repository Pattern
**Purpose:** Abstract data access logic  
**Implementation:** 
- PaymentRepository interface with PaymentRepositoryImpl
- AccountRepository interface with AccountRepositoryImpl
- PaymentAttemptRepository interface with PaymentAttemptRepositoryImpl
- IdempotencyRepository interface with IdempotencyRepositoryImpl

**Code Structure:**
```java
// Interface
public interface PaymentRepository {
    void save(Payment payment);
    Payment findById(UUID id);
    void update(Payment payment);
}

// Implementation
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    private SqlLoader sqlLoader;
    private ReadDao readDao;
    private WriteDao writeDao;
    
    // Implementation methods
}
```

**Benefits:**
- Decouples business logic from database access
- Easy to test (mockable)
- Flexible to change database technology
- Centralized data access logic
- Single responsibility (data access only)

**SOLID Principles Applied:**
- ✅ **Dependency Inversion** - Depends on abstraction (Repository interface)
- ✅ **Single Responsibility** - Only handles data access

---

#### 11.1.2 Factory Pattern
**Purpose:** Create and manage payment provider instances  
**Implementation:** ProviderFactory class

**Code Structure:**
```java
@Component
public class ProviderFactory {
    private Map<Provider, PaymentProvider> providers;
    
    public PaymentProvider getProvider(Provider providerType) {
        return providers.get(providerType);
    }
    
    public List<PaymentProvider> getAvailableProviders() {
        return new ArrayList<>(providers.values());
    }
}
```

**Benefits:**
- Centralized provider creation
- Easy to add new providers
- Decouples provider selection from business logic
- Single point of provider management
- Flexible provider strategy

**SOLID Principles Applied:**
- ✅ **Open/Closed** - Open for extension (new providers), closed for modification
- ✅ **Single Responsibility** - Only handles provider creation
- ✅ **Dependency Inversion** - Returns PaymentProvider interface, not concrete classes

---

#### 11.1.3 Executor Pattern
**Purpose:** Orchestrate complex payment execution logic  
**Implementation:** PaymentExecutor class

**Code Structure:**
```java
@Component
public class PaymentExecutor {
    private ProviderFactory providerFactory;
    private PaymentRepository paymentRepository;
    
    public PaymentResponse execute(Payment payment) {
        List<PaymentProvider> providers = providerFactory.getAvailableProviders();
        
        for (PaymentProvider provider : providers) {
            try {
                PaymentResult result = provider.processPayment(payment);
                logAttempt(payment.getId(), provider, SUCCESS);
                return success;
            } catch (Exception e) {
                logAttempt(payment.getId(), provider, FAILED, e.getMessage());
            }
        }
        
        return failure;
    }
}
```

**Benefits:**
- Separates complex execution logic from service
- Clean service layer
- Encapsulates retry logic
- Handles provider selection and fallback
- Clear responsibility boundaries

**SOLID Principles Applied:**
- ✅ **Single Responsibility** - Only handles payment execution
- ✅ **Dependency Inversion** - Depends on ProviderFactory and Repository interfaces
- ✅ **Open/Closed** - Can add new execution strategies without modifying

---

#### 11.1.4 Decorator Pattern
**Purpose:** Enhance service functionality with cross-cutting concerns  
**Implementation:** Service layer composition

**Code Structure:**
```java
@Service
public class PaymentService {
    private PaymentRepository paymentRepository;
    private AccountService accountService;
    private PaymentExecutor paymentExecutor;
    private IdempotencyRepository idempotencyRepository;
    
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // Validation layer (decorator)
        validateRequest(request);
        
        // Idempotency layer (decorator)
        PaymentResponse cached = checkIdempotency(request.getIdempotencyKey());
        if (cached != null) return cached;
        
        // Business logic layer
        Payment payment = createPaymentEntity(request);
        paymentRepository.save(payment);
        
        // Execution layer (decorator)
        paymentExecutor.execute(payment);
        
        // Logging layer (decorator)
        logPaymentCreated(payment);
        
        return buildResponse(payment);
    }
}
```

**Benefits:**
- Separates concerns (validation, logging, execution)
- Maintains clean service layer
- Easy to add/remove functionality
- Flexible composition
- Clear layering

**SOLID Principles Applied:**
- ✅ **Single Responsibility** - Service coordinates between components
- ✅ **Open/Closed** - Can add new decorators (e.g., caching, monitoring)
- ✅ **Dependency Inversion** - Depends on abstractions

---

#### 11.1.5 Global Exception Handler Pattern
**Purpose:** Centralized error handling and response formatting  
**Implementation:** GlobalExceptionHandler with @ControllerAdvice

**Code Structure:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            404,
            "Payment Not Found",
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAmount(
            InvalidPaymentAmountException ex) {
        ErrorResponse error = new ErrorResponse(
            400,
            "Invalid Payment Amount",
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

**Benefits:**
- Consistent error responses
- Centralized error handling
- DRY (Don't Repeat Yourself)
- Easy to modify error handling strategy
- Clear error contract

**SOLID Principles Applied:**
- ✅ **Single Responsibility** - Only handles exception handling
- ✅ **Open/Closed** - Easy to add new exception handlers
- ✅ **Dependency Inversion** - Depends on exception abstractions

---

#### 11.1.6 Dependency Injection Pattern
**Purpose:** Manage dependencies and promote loose coupling  
**Implementation:** Spring @Autowired, constructor injection

**Code Structure:**
```java
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final PaymentExecutor paymentExecutor;
    private final IdempotencyRepository idempotencyRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    
    // Constructor injection (best practice)
    public PaymentService(
            PaymentRepository paymentRepository,
            AccountService accountService,
            PaymentExecutor paymentExecutor,
            IdempotencyRepository idempotencyRepository,
            PaymentAttemptRepository paymentAttemptRepository) {
        this.paymentRepository = paymentRepository;
        this.accountService = accountService;
        this.paymentExecutor = paymentExecutor;
        this.idempotencyRepository = idempotencyRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
    }
}
```

**Benefits:**
- Loose coupling
- Easy to test (mockable)
- Flexible configuration
- Clear dependencies
- Immutable final fields

**SOLID Principles Applied:**
- ✅ **Dependency Inversion** - Depends on abstractions, not concrete implementations
- ✅ **Interface Segregation** - Each dependency has specific interface

---

### 11.2 SOLID Principles Implementation

#### 11.2.1 Single Responsibility Principle (SRP)

**Definition:** A class should have one, and only one, reason to change.

**How Applied in Payment Orchestrator:**

1. **PaymentService** - Only responsible for orchestrating payment creation
   ```java
   ✅ Responsibilities:
   - Validate payment request
   - Check idempotency
   - Create payment entity
   - Coordinate execution
   - Return response
   
   ❌ NOT Responsible for:
   - Database access (delegated to PaymentRepository)
   - Account management (delegated to AccountService)
   - Payment execution (delegated to PaymentExecutor)
   - Exception handling (delegated to GlobalExceptionHandler)
   ```

2. **PaymentRepository** - Only responsible for payment persistence
   ```java
   ✅ Responsibilities:
   - CRUD operations for payments
   - SQL query building
   - ResultSet mapping
   
   ❌ NOT Responsible for:
   - Business logic
   - Transaction management
   - Validation
   ```

3. **PaymentExecutor** - Only responsible for payment execution
   ```java
   ✅ Responsibilities:
   - Execute payment with providers
   - Handle provider selection
   - Manage retry logic
   - Log attempts
   
   ❌ NOT Responsible for:
   - Account balance management
   - Database updates
   - Request validation
   ```

4. **ProviderFactory** - Only responsible for provider creation
   ```java
   ✅ Responsibilities:
   - Create provider instances
   - Manage provider registry
   - Return available providers
   
   ❌ NOT Responsible for:
   - Payment processing
   - Account management
   - Error handling
   ```

**Benefits of SRP in This Application:**
- Easy to understand each class
- Easy to test (focused unit tests)
- Easy to modify (changes are localized)
- Easy to reuse (classes have single purpose)
- Reduced coupling between classes

---

#### 11.2.2 Open/Closed Principle (OCP)

**Definition:** Software entities should be open for extension but closed for modification.

**How Applied in Payment Orchestrator:**

1. **Payment Provider System** - Open for adding new providers without modifying existing code
   ```java
   // New providers can be added by implementing PaymentProvider interface
   public interface PaymentProvider {
       PaymentResult processPayment(Payment payment);
   }
   
   // Existing implementation (not modified)
   @Component
   public class ProviderA implements PaymentProvider {
       @Override
       public PaymentResult processPayment(Payment payment) {
           // Implementation
       }
   }
   
   // New provider added (no changes to existing code!)
   @Component
   public class ProviderC implements PaymentProvider {
       @Override
       public PaymentResult processPayment(Payment payment) {
           // Implementation for new provider
       }
   }
   ```

2. **Repository Layer** - Open for adding new repositories without modifying existing ones
   ```java
   // New repository can be added
   public interface OrderRepository {
       void save(Order order);
       Order findById(UUID id);
   }
   
   @Repository
   public class OrderRepositoryImpl implements OrderRepository {
       // Implementation
   }
   
   // Existing PaymentRepository remains unchanged
   ```

3. **Exception Handling** - Open for adding new exception types
   ```java
   // New exception handler can be added
   @ExceptionHandler(NewCustomException.class)
   public ResponseEntity<ErrorResponse> handleNewException(
           NewCustomException ex) {
       // Handle new exception
   }
   
   // Existing handlers remain unchanged
   ```

**Benefits of OCP in This Application:**
- Easy to add new payment providers (just implement interface)
- Easy to add new repositories
- Easy to add new exception types
- Existing code remains stable
- Reduced risk of breaking changes

---

#### 11.2.3 Liskov Substitution Principle (LSP)

**Definition:** Subtypes must be substitutable for their base types.

**How Applied in Payment Orchestrator:**

1. **PaymentProvider Implementations**
   ```java
   // All implementations satisfy the contract
   PaymentProvider providerA = new ProviderA();
   PaymentProvider providerB = new ProviderB();
   
   // Can be used interchangeably
   PaymentResult result1 = providerA.processPayment(payment);
   PaymentResult result2 = providerB.processPayment(payment);
   
   // Both are substitutable without breaking code
   for (PaymentProvider provider : providerList) {
       PaymentResult result = provider.processPayment(payment);
       // Works with any provider implementation
   }
   ```

2. **Repository Implementations**
   ```java
   // All repository implementations follow same contract
   PaymentRepository repo1 = new PaymentRepositoryImpl(sqlLoader, readDao, writeDao);
   AccountRepository repo2 = new AccountRepositoryImpl(sqlLoader, readDao, writeDao);
   
   // Can be used interchangeably based on interface
   void processPayment(PaymentRepository repository) {
       repository.save(payment);
       repository.findById(id);
   }
   ```

3. **Service Implementations**
   ```java
   // All services follow same contract
   PaymentService paymentService = new PaymentServiceImpl(repositories...);
   AccountService accountService = new AccountServiceImpl(repositories...);
   
   // Can be mocked for testing
   PaymentService mockService = mock(PaymentService.class);
   // Mock behaves exactly like real implementation
   ```

**Benefits of LSP in This Application:**
- Polymorphic behavior works correctly
- Easy to mock for testing
- Implementations are truly interchangeable
- No surprising behavior changes
- Reliable abstraction contracts

---

#### 11.2.4 Interface Segregation Principle (ISP)

**Definition:** Clients should not be forced to depend on interfaces they don't use.

**How Applied in Payment Orchestrator:**

1. **Small, Focused Interfaces**
   ```java
   // ✅ Good: Small focused interface
   public interface ReadDao {
       <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);
       default <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params) {
           // Implementation
       }
   }
   
   // ✅ Good: Small focused interface
   public interface WriteDao {
       int update(String sql, Object... params);
   }
   
   // ❌ Bad: Would be large, unfocused interface
   public interface Dao {
       <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);
       int update(String sql, Object... params);
       void delete(String sql, Object... params);
       void batch(String sql, List<Object[]> params);
       // ... more methods
   }
   ```

2. **Specific Repository Interfaces**
   ```java
   // ✅ Good: Each repository only has methods it needs
   public interface PaymentRepository {
       void save(Payment payment);
       Payment findById(UUID id);
       Payment findByIdempotencyKey(String key);
       void update(Payment payment);
   }
   
   // Payment service only depends on these methods
   @Service
   public class PaymentService {
       private final PaymentRepository paymentRepository;
       
       // Only uses methods defined in PaymentRepository interface
       public void createPayment(CreatePaymentRequest request) {
           paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());
           paymentRepository.save(payment);
           paymentRepository.update(payment);
       }
   }
   ```

3. **Service Interfaces**
   ```java
   // ✅ Good: Segregated service interfaces
   public interface PaymentService {
       PaymentResponse createPayment(CreatePaymentRequest request);
       PaymentDetailsResponse getPayment(UUID paymentId);
   }
   
   public interface AccountService {
       Account getAccount(UUID accountId);
       void deductBalance(UUID accountId, Long amount);
       void creditBalance(UUID accountId, Long amount);
   }
   
   // Each service is independent and focused
   // Clients only depend on methods they actually use
   ```

**Benefits of ISP in This Application:**
- Interfaces are focused and minimal
- Clients not forced to implement unused methods
- Easy to mock specific interfaces
- Clear contract between components
- Reduced coupling
- More reusable interfaces

---

#### 11.2.5 Dependency Inversion Principle (DIP)

**Definition:** High-level modules should not depend on low-level modules. Both should depend on abstractions.

**How Applied in Payment Orchestrator:**

1. **Service Depends on Repository Interface, Not Implementation**
   ```java
   // ✅ Good: High-level service depends on abstraction
   @Service
   public class PaymentService {
       private final PaymentRepository paymentRepository; // Abstraction!
       
       public PaymentService(PaymentRepository paymentRepository) {
           this.paymentRepository = paymentRepository;
       }
       
       public void createPayment(CreatePaymentRequest request) {
           paymentRepository.save(payment); // Uses interface
       }
   }
   
   // ❌ Bad: High-level service depends on concrete implementation
   @Service
   public class BadPaymentService {
       private final PaymentRepositoryImpl repository; // Concrete!
       
       public void createPayment(CreatePaymentRequest request) {
           repository.save(payment); // Tightly coupled
       }
   }
   ```

2. **Controller Depends on Service Interface**
   ```java
   // ✅ Good: Controller depends on service abstraction
   @RestController
   @RequestMapping("/payments")
   public class PaymentController {
       private final PaymentService paymentService; // Abstraction!
       
       public PaymentController(PaymentService paymentService) {
           this.paymentService = paymentService;
       }
       
       @PostMapping
       public ResponseEntity<PaymentResponse> createPayment(
               @Valid @RequestBody CreatePaymentRequest request) {
           PaymentResponse response = paymentService.createPayment(request);
           return ResponseEntity.status(HttpStatus.CREATED).body(response);
       }
   }
   ```

3. **Executor Depends on Factory Interface**
   ```java
   // ✅ Good: Executor depends on factory abstraction
   @Component
   public class PaymentExecutor {
       private final ProviderFactory providerFactory; // Abstraction!
       
       public PaymentExecutor(ProviderFactory providerFactory) {
           this.providerFactory = providerFactory;
       }
       
       public PaymentResponse execute(Payment payment) {
           List<PaymentProvider> providers = providerFactory.getAvailableProviders();
           // Works with any provider implementation
       }
   }
   ```

4. **Dependency Injection Configuration**
   ```java
   // ✅ Good: Spring configuration manages dependencies
   @Configuration
   public class PaymentConfig {
       
       @Bean
       public PaymentRepository paymentRepository(
               SqlLoader sqlLoader,
               ReadDao readDao,
               WriteDao writeDao) {
           return new PaymentRepositoryImpl(sqlLoader, readDao, writeDao);
       }
       
       @Bean
       public PaymentService paymentService(
               PaymentRepository paymentRepository,
               AccountService accountService,
               PaymentExecutor paymentExecutor,
               IdempotencyRepository idempotencyRepository,
               PaymentAttemptRepository paymentAttemptRepository) {
           return new PaymentServiceImpl(
               paymentRepository,
               accountService,
               paymentExecutor,
               idempotencyRepository,
               paymentAttemptRepository
           );
       }
   }
   ```

**Dependency Flow (Correct - DIP Applied):**
```
Controller
    ↓ (depends on abstraction)
PaymentService (interface)
    ↓ (depends on abstraction)
PaymentRepository (interface) + PaymentExecutor + AccountService
    ↓ (depends on abstraction)
ReadDao/WriteDao (interface)
    ↓ (depends on concrete)
JDBC/Database
```

**Benefits of DIP in This Application:**
- Easy to test (mock abstractions)
- Easy to swap implementations
- Flexible configuration
- Reduced coupling
- Better reusability
- Easier dependency management

---

### 11.3 Design Patterns & SOLID Principles Summary

| Pattern/Principle | Application | Benefit |
|---|---|---|
| **Repository Pattern** | PaymentRepository, AccountRepository | Decouples data access from business logic |
| **Factory Pattern** | ProviderFactory | Centralized provider creation |
| **Executor Pattern** | PaymentExecutor | Separates complex logic from service |
| **Decorator Pattern** | Service composition | Cross-cutting concerns |
| **Exception Handler** | GlobalExceptionHandler | Centralized error handling |
| **Dependency Injection** | Constructor injection | Loose coupling, easy testing |
| **SRP** | Each class has one responsibility | Easy to understand, test, modify |
| **OCP** | New providers via interface | Easy extension, stable code |
| **LSP** | Substitutable implementations | Reliable polymorphism |
| **ISP** | Small focused interfaces | Clients use only what they need |
| **DIP** | Depends on abstractions | Flexible, testable, maintainable |

---

### 11.4 SOLID Principles Verification Checklist

✅ **Single Responsibility**
- PaymentService: Orchestration only
- PaymentRepository: Data access only
- PaymentExecutor: Execution only
- ProviderFactory: Creation only

✅ **Open/Closed**
- New providers can be added (implement interface)
- New repositories can be added (implement interface)
- New exception handlers can be added (no modification of existing)
- New decorators can be added (service composition)

✅ **Liskov Substitution**
- PaymentProvider implementations are truly interchangeable
- Repository implementations follow same contract
- Service implementations can be mocked without issues

✅ **Interface Segregation**
- ReadDao: Only query operations
- WriteDao: Only update operations
- PaymentRepository: Only payment operations
- AccountService: Only account operations

✅ **Dependency Inversion**
- Services depend on repository interfaces
- Controllers depend on service interfaces
- Executor depends on factory abstraction
- Constructor injection of dependencies

---

## 12. Code Coverage

### 12.1 Coverage Metrics

| Metric | Coverage | Target | Status |
|--------|----------|--------|--------|
| **Instruction** | 91% | 90% | ✅ Pass |
| **Branch** | 95% | 90% | ✅ Pass |
| **Line** | 93% | - | ✅ Good |
| **Method** | 98% | - | ✅ Excellent |
| **Class** | 97% | - | ✅ Excellent |

### 12.2 Package-wise Coverage

| Package | Instructions | Branches | Status |
|---------|---|---|---|
| com.yuno.payment.service | 100% | 100% | ✅ Perfect |
| com.yuno.payment.executor | 100% | 100% | ✅ Perfect |
| com.yuno.payment.controller | 100% | n/a | ✅ Perfect |
| com.yuno.payment.factory | 100% | 100% | ✅ Perfect |
| com.yuno.payment.model | 100% | 93% | ✅ Good |
| com.yuno.payment.dao | 90% | 50% | ⚠️ Needs work |
| com.yuno.payment.mapper | 84% | 100% | ⚠️ Needs work |
| com.yuno.payment.repository | 75% | 100% | ⚠️ Needs work |

### 12.3 Test Statistics

- Total test classes: 20+
- Total test methods: 100+
- Code coverage tools: JaCoCo 0.8.13
- Coverage reports: HTML + CSV + XML

---

## 13. Appendix

### A. Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 4.0.5 |
| **Language** | Java | 17 |
| **Database** | PostgreSQL | 12+ |
| **Build Tool** | Maven | 3.6+ |
| **Testing** | JUnit 5, Mockito | Latest |
| **Logging** | SLF4J + Logback | Latest |
| **Code Coverage** | JaCoCo | 0.8.13 |
| **Data Access** | JDBC, JdbcTemplate | Spring |

### B. Build & Run Commands

**Build Project:**
```bash
./mvnw clean install
```

**Run Tests:**
```bash
./mvnw clean test
```

**Generate Coverage Report:**
```bash
./mvnw clean verify
```

**Run Application:**
```bash
./mvnw spring-boot:run
```

### C. Key Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
</dependency>
```

### D. Configuration Example

```yaml
spring:
  application:
    name: payment-orchestrator
  datasource:
    url: jdbc:postgresql://localhost:5432/payments
    username: postgres
    password: password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
server:
  port: 8080
  servlet:
    context-path: /
    
logging:
  level:
    com.yuno.payment: INFO
    org.springframework.web: DEBUG
```

### E. Future Enhancements

- [ ] Payment webhook notifications
- [ ] Real-time status updates (WebSocket)
- [ ] Payment scheduling
- [ ] Advanced fraud detection
- [ ] Multi-currency conversion
- [ ] Payment reversals/refunds
- [ ] Analytics dashboard
- [ ] Admin APIs
- [ ] Rate limiting
- [ ] Caching layer (Redis)

---

## 14. Summary

The Payment Orchestrator is a well-architected, production-ready microservice that demonstrates:

- **Clean Architecture:** Clear separation of concerns across layers
- **Design Patterns:** Proven patterns (Repository, Factory, Executor)
- **High Quality:** 91% code coverage exceeding 90% target
- **Fault Tolerance:** Automatic retry with fallback providers
- **Data Integrity:** Transaction handling and idempotency support
- **Error Handling:** Comprehensive exception handling and logging
- **Scalability:** Thread-safe operations with connection pooling
- **Maintainability:** Well-documented, tested, and organized code

This documentation provides a complete understanding of the system for developers, architects, and DevOps teams to successfully work with and maintain the Payment Orchestrator microservice.

---

**End of Document**

Document Version: 1.0  
Last Updated: April 10, 2026  
Total Pages: ~50 (when converted to PDF)  

---


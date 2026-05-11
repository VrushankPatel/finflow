# Finflow Agents

This document describes the agents (microservices) that make up the Finflow financial transaction system.

## Overview

Finflow is a distributed financial transaction system built with Spring Boot microservices architecture. The system processes financial transactions using double-entry ledger principles, ensures idempotency for safe retries, and generates account statements through event-driven communication.

---

## Agents

### API Gateway

**Port:** 8080  
**Purpose:** Entry point for all client requests, routing traffic to downstream services

**Responsibilities:**
- Route requests to appropriate services based on URL paths
- `/api/transactions/**` → Transaction Service
- `/api/statements/**` → Statement Service

**Technology:** Spring Cloud Gateway

---

### Transaction Service

**Port:** 8081  
**Purpose:** Core financial transaction processing engine

**Responsibilities:**
- Process financial transactions between accounts
- Implement double-entry ledger accounting
- Maintain idempotency to prevent duplicate processing
- Emit transaction completion events to Kafka
- Manage account balances and ledger entries

**Key Features:**
- **Idempotency:** Uses idempotency keys to safely retry transactions without double-processing
- **Double-Entry Ledger:** Every transaction creates both debit and credit entries for audit trail
- **Event Publishing:** Emits `TransactionCompletedEvent` to Kafka topic `transaction-completed`

**Database:** H2 in-memory database (transaction-db)

**Kafka Topics:**
- Produces to: `transaction-completed`

**Domain Models:**
- `Account` - User account with balance and currency
- `LedgerEntry` - Audit trail entry (debit/credit)
- `IdempotencyKey` - Tracks processed requests

---

### Statement Service

**Port:** 8082  
**Purpose:** Generate and manage account statements

**Responsibilities:**
- Generate account statements on demand
- Consume transaction events from Kafka for real-time updates
- Store statement metadata and S3 URLs

**Kafka Topics:**
- Consumes from: `transaction-completed`
- Consumer Group: `statement-group`

**Database:** H2 in-memory database (statement-db)

**Domain Models:**
- `Statement` - Generated statement with content and S3 URL

---

### Commons Module

**Purpose:** Shared domain models and events across services

**Components:**
- `BaseEvent` - Base class for all domain events
- `TransactionCompletedEvent` - Event emitted when a transaction completes
- `TransactionStatus` - Enum for transaction states
- `Money` - Value object for monetary amounts

---

## Communication Flow

```
Client Request
    ↓
API Gateway (8080)
    ↓
    ├─→ Transaction Service (8081)
    │       ↓
    │   Process Transaction
    │       ↓
    │   Update Ledger
    │       ↓
    │   Emit Event → Kafka
    │
    └─→ Statement Service (8082)
            ↓
        Generate Statement
            ↓
        Store in S3 (mocked)
```

**Event Flow:**
1. Transaction Service processes transaction
2. Transaction Service emits `TransactionCompletedEvent` to Kafka
3. Statement Service consumes event for potential statement updates

---

## Technology Stack

- **Framework:** Spring Boot 3.3.3
- **Java Version:** 17
- **Spring Cloud:** 2023.0.3
- **Message Broker:** Apache Kafka
- **Database:** H2 (in-memory)
- **Build Tool:** Maven
- **API Gateway:** Spring Cloud Gateway

---

## Running the System

**Prerequisites:**
- Java 17+
- Maven
- Kafka cluster (localhost:19092, 19093, 19094)

**Build:**
```bash
mvn clean install
```

**Run Services:**
```bash
# Terminal 1 - API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 2 - Transaction Service
cd transaction-service && mvn spring-boot:run

# Terminal 3 - Statement Service
cd statement-service && mvn spring-boot:run
```

---

## API Endpoints

### Transaction Service
- `POST /api/transactions` - Process a new transaction

### Statement Service
- `GET /api/statements/{accountId}` - Generate statement for account

---

## Design Patterns

- **Idempotency Pattern:** Safe retry of failed requests
- **Double-Entry Ledger:** Financial accounting with audit trail
- **Event-Driven Architecture:** Decoupled services via Kafka
- **API Gateway Pattern:** Single entry point with routing
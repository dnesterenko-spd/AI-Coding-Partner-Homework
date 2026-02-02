# 🏦 Banking Transactions API

> **Framework**: Java Spring Boot
> **Port**: 8080
> **AI Tools Used**: GitHub Copilot
> **Date**: January 2026

---

## 📋 Project Overview

A comprehensive REST API for managing banking transactions with full validation, filtering, and account management capabilities. Built with Java Spring Boot, this API provides a robust foundation for transaction processing with in-memory storage.

### ✨ Key Features Implemented

#### ✅ **Task 1: Core API (Required)**
- **POST `/transactions`** - Create new transactions (deposit, withdrawal, transfer)
- **GET `/transactions`** - Retrieve all transactions
- **GET `/transactions/{id}`** - Get specific transaction by ID
- **GET `/accounts/{accountId}/balance`** - Get account balance

#### ✅ **Task 2: Transaction Validation (Required)**
- Amount validation (positive numbers, max 2 decimal places)
- Account number format validation (`ACC-XXXXX` pattern)
- Currency validation (20+ ISO 4217 currency codes supported)
- Type validation (DEPOSIT, WITHDRAWAL, TRANSFER)
- Comprehensive error response with field-level details

#### ✅ **Task 3: Basic Transaction History (Required)**
- Filter by account ID: `?accountId=ACC-12345`
- Filter by transaction type: `?type=TRANSFER`
- Filter by date range: `?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59`
- Combine multiple filters

#### 🌟 **Additional Feature: Transaction Summary Endpoint (Option A)**
- **GET `/accounts/{accountId}/summary`** - Get comprehensive account statistics
  - Total deposits amount
  - Total withdrawals amount
  - Transaction count
  - Most recent transaction date

---

## 🏗️ Architecture

### Project Structure
```
homework-1/
├── pom.xml                          # Maven configuration
├── .gitignore                       # Git ignore rules
├── src/main/java/com/banking/
│   ├── BankingApiApplication.java   # Main Spring Boot application
│   ├── controller/
│   │   ├── TransactionController.java      # Transaction endpoints
│   │   └── AccountController.java          # Account endpoints
│   ├── service/
│   │   └── TransactionService.java         # Business logic
│   ├── repository/
│   │   └── TransactionRepository.java      # In-memory storage + seed data
│   ├── model/
│   │   ├── Transaction.java
│   │   ├── TransactionType.java
│   │   └── TransactionStatus.java
│   ├── dto/
│   │   ├── CreateTransactionRequest.java
│   │   ├── TransactionResponse.java
│   │   ├── BalanceResponse.java
│   │   ├── TransactionSummaryResponse.java
│   │   └── ValidationErrorResponse.java
│   ├── validator/
│   │   └── TransactionValidator.java       # Validation logic
│   └── exception/
│       ├── GlobalExceptionHandler.java     # Error handling
│       ├── ResourceNotFoundException.java
│       └── BadRequestException.java
├── src/main/resources/
│   └── application.properties       # Configuration
└── docs/
    └── screenshots/
```

### Design Patterns Used
- **MVC Pattern**: Separation of concerns with Controllers, Services, and Repositories
- **Validator Pattern**: Custom validation logic for business rules
- **DTO Pattern**: Request/response data transfer objects
- **Exception Handling**: Global exception handler for consistent error responses
- **Repository Pattern**: Abstraction for data access layer

---

## 📊 Seed Data

The application starts with pre-populated sample transactions:

```json
{
  "sampleAccounts": ["ACC-12345", "ACC-67890", "ACC-11111"],
  "sampleTransactions": 7,
  "seedData": {
    "deposits": 3,
    "withdrawals": 1,
    "transfers": 3
  }
}
```

Sample accounts and their initial balances:
- **ACC-12345**: $249.50 USD
- **ACC-67890**: €224.75 EUR  
- **ACC-11111**: £1,200.00 GBP

---

## 🔌 API Endpoints Reference

### Transaction Management

#### Create Transaction
```http
POST /transactions
Content-Type: application/json

{
  "fromAccount": "ACC-12345",
  "toAccount": "ACC-67890",
  "amount": 100.50,
  "currency": "USD",
  "type": "TRANSFER"
}
```

#### Get All Transactions (with filters)
```http
GET /transactions
GET /transactions?accountId=ACC-12345
GET /transactions?type=TRANSFER
GET /transactions?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
GET /transactions?accountId=ACC-12345&type=TRANSFER
```

#### Get Transaction by ID
```http
GET /transactions/{id}
```

### Account Management

#### Get Account Balance
```http
GET /accounts/ACC-12345/balance
```

Response:
```json
{
  "accountId": "ACC-12345",
  "balance": 249.50,
  "currency": "USD"
}
```

#### Get Account Summary
```http
GET /accounts/ACC-12345/summary
```

Response:
```json
{
  "accountId": "ACC-12345",
  "totalDeposits": 500.00,
  "totalWithdrawals": 100.00,
  "transactionCount": 3,
  "mostRecentTransactionDate": "2024-01-29T16:30:00"
}
```

---

## ✅ Validation Rules

### Amount Validation
- Must be positive (> 0)
- Maximum 2 decimal places
- Required field

### Account Number Format
- Must follow pattern: `ACC-XXXXX` (where X is alphanumeric)
- Format examples: `ACC-12345`, `ACC-ABC99`

### Currency Validation
Supported currencies (ISO 4217):
```
USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, SEK, NZD,
MXN, SGD, HKD, NOK, KRW, TRY, RUB, INR, BRL, ZAR
```

### Transaction Type
- DEPOSIT: Money added to account (only toAccount required)
- WITHDRAWAL: Money removed from account (only fromAccount required)
- TRANSFER: Money moved between accounts (both accounts required, must be different)

---

## 🛠️ Technologies Used

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 17+ |
| Build Tool | Maven | 3.8+ |
| JSON Processing | Jackson | Built-in |
| Validation | Spring Validation | Built-in |
| Data Structures | Java Collections | Built-in |

---

## 📝 Error Handling

### Validation Error Response
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be a positive number"
    },
    {
      "field": "currency",
      "message": "Invalid currency code: XXX"
    }
  ]
}
```

### HTTP Status Codes
- **200 OK**: Successful GET request
- **201 Created**: Transaction created successfully
- **400 Bad Request**: Validation failed or invalid parameters
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Unexpected server error

---

## 🧪 Testing the API

### Using the provided demo files:

1. **Run the shell script** (includes sample requests):
   ```bash
   chmod +x demo/sample-requests.sh
   ./demo/sample-requests.sh
   ```

2. **Use the HTTP file** with VS Code REST Client:
   - Install REST Client extension
   - Open `demo/sample-requests.http`
   - Click "Send Request" on any request

3. **Use curl** directly:
   ```bash
   # Get all transactions
   curl http://localhost:8080/transactions
   
   # Get balance for account
   curl http://localhost:8080/accounts/ACC-12345/balance
   ```

---

## 🤖 AI-Assisted Development

This project was developed using **GitHub Copilot** for code generation and assistance:

### Key Prompts Used:
1. "Generate Spring Boot REST API controller for banking transactions"
2. "Create validation logic for transaction amounts and currency codes"
3. "Implement filtering logic for transactions by account and date range"
4. "Create exception handlers for RESTful API"

### Development Process:
- ✅ Generated DTOs and model classes
- ✅ Implemented service layer with business logic
- ✅ Created validation framework
- ✅ Built REST controllers with error handling
- ✅ Added comprehensive documentation

---

## 📚 Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Web MVC](https://spring.io/guides/gs/spring-boot/)
- [ISO 4217 Currency Codes](https://en.wikipedia.org/wiki/ISO_4217)
- [RESTful API Best Practices](https://restfulapi.net/)

---

## 📄 Files Included

- **Source Code**: Complete Java Spring Boot implementation
- **Configuration**: `pom.xml` and `application.properties`
- **Documentation**: README.md, HOWTORUN.md
- **Demo Files**: Sample requests (HTTP and shell scripts)
- **Screenshots**: AI interactions and API testing results

---

<div align="center">

### ✨ A modern REST API for banking transactions, built with Java Spring Boot

**Status**: ✅ Complete - All required tasks implemented + additional feature

</div>

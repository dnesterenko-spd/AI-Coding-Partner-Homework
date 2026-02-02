# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Banking Transactions API** - A Spring Boot REST API for managing banking transactions with comprehensive validation, filtering, and account management. Uses in-memory storage with seed data.

- **Language**: Java 17+
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Port**: 8080

## Common Development Commands

### Build and Run
```bash
# Build the project
mvn clean install

# Run the application (development)
mvn spring-boot:run

# Run the built JAR
java -jar target/banking-api-1.0.0.jar

# Package without running tests
mvn package -DskipTests
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=TransactionControllerTest

# Run a specific test method
mvn test -Dtest=TransactionControllerTest#testCreateTransaction
```

### Project Verification
```bash
# Check if API is running
curl http://localhost:8080/transactions

# Get sample account balance
curl http://localhost:8080/accounts/ACC-12345/balance

# Get account summary
curl http://localhost:8080/accounts/ACC-12345/summary
```

### Development Utilities
```bash
# Clean build artifacts
mvn clean

# Format code (if configured)
mvn spotless:apply

# Check for dependency issues
mvn dependency:tree
```

## Architecture Overview

### Layered Architecture Pattern

The application follows a strict **Controller → Validator → Service → Repository** pattern:

```
HTTP Request
    ↓
TransactionController / AccountController (REST endpoints, request routing)
    ↓
TransactionValidator (validation rules, error collection)
    ↓
TransactionService (business logic, filtering, calculations)
    ↓
TransactionRepository (in-memory data storage, seed data)
    ↓
Response DTOs (JSON serialization)
```

### Key Design Patterns

1. **Repository Pattern**: `TransactionRepository` provides abstraction over in-memory storage using `CopyOnWriteArrayList` for thread-safe operations
2. **Service Layer**: `TransactionService` encapsulates all business logic - balance calculations, filtering, summary aggregations
3. **Validator Pattern**: `TransactionValidator` performs all validation upfront, returning field-level error details
4. **DTO Pattern**: Request/response objects (`CreateTransactionRequest`, `TransactionResponse`, etc.) decouple API contracts from domain models
5. **Global Exception Handling**: `GlobalExceptionHandler` catches exceptions and returns consistent error responses

### Core Components

**Controllers** (`com.banking.controller/`):
- `TransactionController`: Handles `/transactions` endpoints (create, list, get by ID)
- `AccountController`: Handles `/accounts/{id}/*` endpoints (balance, summary)

**Service** (`com.banking.service/TransactionService.java`):
- Creates transactions (applies timestamp, UUID, COMPLETED status)
- Retrieves transactions with multi-filter support (accountId, type, date range)
- Calculates account balances accounting for transaction direction (deposits/withdrawals + transfers)
- Generates account summaries (totals and transaction counts)

**Validator** (`com.banking.validator/TransactionValidator.java`):
- Validates amount (positive, max 2 decimal places)
- Validates currency (20 ISO 4217 codes: USD, EUR, GBP, etc.)
- Validates account number format (`ACC-XXXXX` pattern)
- Validates transaction type (DEPOSIT, WITHDRAWAL, TRANSFER)
- Enforces type-specific requirements (TRANSFER needs both accounts, DEPOSIT needs toAccount only, etc.)
- Returns `ValidationErrorResponse` with all errors collected

**Repository** (`com.banking.repository/TransactionRepository.java`):
- Thread-safe in-memory storage using `CopyOnWriteArrayList`
- Initializes with 7 seed transactions across 3 sample accounts
- Provides query methods: `findById`, `findAll`, `findByAccountId`, `findByType`, `findByDateRange`

### Data Model

**Transaction** (`com.banking.model/Transaction.java`):
- `id`: UUID string
- `fromAccount`: Account (nullable, required for WITHDRAWAL/TRANSFER)
- `toAccount`: Account (nullable, required for DEPOSIT/TRANSFER)
- `amount`: BigDecimal (always positive)
- `currency`: ISO 4217 code (uppercase)
- `type`: TransactionType enum (DEPOSIT, WITHDRAWAL, TRANSFER)
- `timestamp`: LocalDateTime (set to now on creation)
- `status`: TransactionStatus enum (always COMPLETED on creation)

### Important Implementation Details

**Balance Calculation Logic** (in `TransactionService`):
- DEPOSIT or incoming TRANSFER → add to balance
- WITHDRAWAL or outgoing TRANSFER → subtract from balance
- Only COMPLETED transactions count
- Transactions hold the currency; balance defaults to USD if account has no transactions

**Multi-Filter Logic** (in `getAllTransactions`):
- Filters are applied sequentially (accountId → type → date range)
- Filters are optional and can be combined
- Invalid transaction type returns empty list
- Date range uses inclusive boundaries (LocalDateTime.MIN/MAX for open-ended ranges)

**Account Matching** (in `findByAccountId`):
- A transaction matches if account appears as either fromAccount OR toAccount
- Used for both balance calculation and summary aggregation

## API Endpoints Reference

### Transactions
- `POST /transactions` - Create transaction (returns 201 CREATED)
- `GET /transactions` - List all (supports filters: ?accountId=, ?type=, ?from=, ?to=)
- `GET /transactions/{id}` - Get by ID

### Accounts
- `GET /accounts/{accountId}/balance` - Account balance
- `GET /accounts/{accountId}/summary` - Aggregated account statistics

### Sample Seed Accounts
- `ACC-12345`: $249.50 USD
- `ACC-67890`: €224.75 EUR
- `ACC-11111`: £1,200.00 GBP

## Validation Rules

### Amount
- Must be positive (> 0)
- Maximum 2 decimal places
- Required field

### Account Number Format
- Pattern: `ACC-XXXXX` where X is alphanumeric (A-Z, a-z, 0-9)
- Examples: `ACC-12345`, `ACC-ABC99`

### Currency
- Must be one of 20 supported ISO 4217 codes
- Supported: USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, SEK, NZD, MXN, SGD, HKD, NOK, KRW, TRY, RUB, INR, BRL, ZAR

### Transaction Type
- **DEPOSIT**: Add funds to toAccount (fromAccount must be null)
- **WITHDRAWAL**: Remove funds from fromAccount (toAccount must be null)
- **TRANSFER**: Move funds from fromAccount to toAccount (both required, must be different)

### Error Response Format
```json
{
  "error": "Validation failed",
  "details": [
    {"field": "amount", "message": "Amount must be a positive number"},
    {"field": "currency", "message": "Invalid currency code: XXX"}
  ]
}
```

## File Structure

```
src/main/java/com/banking/
├── BankingApiApplication.java        # @SpringBootApplication entry point
├── controller/
│   ├── TransactionController.java     # @RestController for /transactions
│   └── AccountController.java         # @RestController for /accounts
├── service/
│   └── TransactionService.java        # @Service (business logic)
├── repository/
│   └── TransactionRepository.java     # @Repository (data access, seed data)
├── model/
│   ├── Transaction.java               # Domain model
│   ├── TransactionType.java           # Enum: DEPOSIT, WITHDRAWAL, TRANSFER
│   └── TransactionStatus.java         # Enum: COMPLETED
├── dto/
│   ├── CreateTransactionRequest.java  # Request payload
│   ├── TransactionResponse.java       # Transaction response
│   ├── BalanceResponse.java           # Balance endpoint response
│   ├── TransactionSummaryResponse.java# Summary endpoint response
│   └── ValidationErrorResponse.java   # Error response
├── validator/
│   └── TransactionValidator.java      # @Component (validation rules)
└── exception/
    ├── GlobalExceptionHandler.java    # @RestControllerAdvice
    ├── ResourceNotFoundException.java # Custom exception (404)
    └── BadRequestException.java       # Custom exception (400)

src/main/resources/
└── application.properties             # Spring configuration
```

## Key Technology Choices

**BigDecimal for Money**: All monetary amounts use `BigDecimal` to avoid floating-point precision issues. This is essential for financial calculations.

**CopyOnWriteArrayList for In-Memory Storage**: Thread-safe without locks, suitable for a read-heavy API with occasional writes.

**LocalDateTime for Timestamps**: Uses LocalDateTime (not Instant) for transaction timestamps. Set to `LocalDateTime.now()` on creation.

**Stream API for Filtering**: Filtering uses streams extensively for clean, functional-style operations. All filtering is in-memory after repository fetch.

**UUID for Transaction IDs**: Transaction IDs are generated as UUID strings (`UUID.randomUUID().toString()`).

## Development Notes

### Adding New Validation Rules
1. Add validation logic to `TransactionValidator.validate()`
2. Create new `ValidationErrorResponse.ValidationError` and add to errors list
3. Validation errors are collected and returned as a batch

### Adding New Endpoints
1. Create method in appropriate controller (`TransactionController` or `AccountController`)
2. Add `@GetMapping`, `@PostMapping`, etc. with path
3. Call service methods and return `ResponseEntity` with appropriate HTTP status
4. Let `GlobalExceptionHandler` handle exceptions

### Adding New Filters
1. Add parameter to `TransactionService.getAllTransactions()` method
2. Add filtering logic as a stream operation
3. Add optional `@RequestParam` to controller method
4. Consider how filter interacts with existing filters

### Seed Data
- Located in `TransactionRepository.initializeSampleData()`
- Uses `LocalDateTime.now().minusDays()` for relative timestamps
- UUIDs generated per transaction
- Status always set to COMPLETED
- Modify here to add/remove sample transactions for testing

## Testing Considerations

- Service layer tests should mock `TransactionRepository`
- Controller tests should mock `TransactionService` and `TransactionValidator`
- Integration tests can use real repository with in-memory data
- Validation tests should verify all error cases and error message content
- Filtering tests should verify combination of multiple filters
- Balance calculation tests need transactions with different types and directions
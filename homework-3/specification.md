# Virtual Card Lifecycle Management System - Specification

## High-Level Objective

Build a PCI-DSS and GDPR compliant virtual card lifecycle management API that enables secure card creation, state management (freeze/unfreeze), spending controls, and transaction history with comprehensive audit logging, fraud prevention, and regulatory compliance.

---

## Mid-Level Objectives

### 1. Implement PCI-DSS Compliant Card Data Handling
Create secure card data storage and processing using AES-256-GCM encryption for Primary Account Numbers (PAN), tokenization for external references, and strict access controls. Never store CVV data per PCI-DSS requirements. Implement TLS 1.3 for all data in transit and comprehensive access logging for all card data operations.

### 2. Create RESTful API for Card Lifecycle Operations
Design and implement a complete RESTful API supporting six core card operations: create virtual card with spending limits, retrieve card details (masked PAN only), freeze/unfreeze card state, update spending limits, view transaction history with filtering, and close card. All endpoints must follow REST conventions with proper HTTP methods and status codes.

### 3. Implement Comprehensive Audit Logging
Build an immutable, append-only audit logging system that captures all state-changing operations with before/after snapshots, actor identification, IP addresses, and timestamps. Implement cryptographic hash chaining (SHA-256) for tamper detection and enforce 7-year minimum retention per SOX compliance requirements.

### 4. Add GDPR-Compliant Data Handling
Implement EU data protection requirements including right to deletion (anonymization, not hard delete to preserve audit trails), right to data export (JSON format), 7-year retention with automatic purging, purpose limitation for data access, and consent management. Ensure all PII can be anonymized while maintaining audit integrity.

### 5. Enforce Decimal Precision for All Monetary Values
Use arbitrary-precision decimal arithmetic (decimal.js library) for all financial calculations to prevent floating-point errors. Validate maximum 2 decimal places for all monetary inputs, implement banker's rounding (round half to even), and store monetary values as strings or NUMERIC type (never FLOAT) in the database.

### 6. Implement Fraud Prevention with Rate Limiting
Build multi-tier rate limiting using Redis: 100 requests/minute per user, 1000 requests/minute global limit, and velocity checks (maximum 5 card creations per user per 24 hours). Return proper 429 status codes with Retry-After headers. Implement distributed rate limiting for horizontal scalability.

### 7. Ensure Idempotent Operations with Atomic Transactions
Implement UUID-based idempotency keys for all state-changing operations with 24-hour deduplication window. Use database transactions (ACID guarantees) to ensure atomic state changes. Implement optimistic locking for concurrent operation handling and store before/after state snapshots in audit logs.

### 8. Create Secure Card Transaction History API
Build transaction history endpoint with support for filtering (date range, amount range, merchant), pagination (maximum 50 items per page), and sorting. Use decimal.js for amount comparisons. Include comprehensive metadata (transaction ID, amount, merchant, timestamp, status) and ensure users can only access their own transactions.

---

## Implementation Notes

### Technology Stack
- **Runtime:** Node.js 20+ LTS (long-term support for production stability)
- **Language:** TypeScript 5+ with strict mode enabled (tsconfig.json: `strict: true`, `noImplicitAny: true`, `strictNullChecks: true`)
- **Framework:** Express.js 4.x (proven, lightweight framework for REST APIs)
- **Database:** PostgreSQL 15+ (ACID compliance, row-level security, advanced indexing)
- **ORM:** Prisma 5+ (type-safe queries, migration management, SQL injection prevention)
- **Validation:** Zod 3+ (TypeScript-first schema validation with custom refinements)
- **Financial Math:** decimal.js (arbitrary-precision decimal arithmetic, prevents 0.1 + 0.2 !== 0.3 issues)
- **Encryption:** Node.js crypto module (AES-256-GCM with initialization vectors)
- **Logging:** Winston 3+ (structured logging, multiple transports, log levels)
- **Testing:** Jest 29+ with Supertest (unit tests, integration tests, security tests)

### PCI-DSS Requirements (Payment Card Industry Data Security Standard)
- **Encryption at Rest:** AES-256-GCM for all card PAN data with unique initialization vectors per encryption
- **Tokenization:** Generate unique card_token (UUID v4) for external references; never expose raw PAN in API responses
- **CVV Prohibition:** NEVER store CVV/CVC/CID data, even encrypted (PCI-DSS SAQ A requirement violation)
- **TLS 1.3:** All data in transit must use TLS 1.3 or higher (disable TLS 1.0, 1.1, 1.2)
- **Access Logging:** Log every access to card data (who, when, what, IP address) to audit table
- **Scope Segmentation:** Isolate card data processing from other system components
- **Security Testing:** Annual penetration testing and quarterly vulnerability scanning required

### GDPR Requirements (General Data Protection Regulation)
- **7-Year Retention:** Maintain data for 7 years per financial regulations, then automatic purge via cron job
- **Right to Deletion:** DELETE /api/v1/users/me endpoint anonymizes PII (name → "DELETED_USER") while preserving audit logs
- **Right to Export:** GET /api/v1/users/me/data endpoint returns complete user data (profile, cards masked, transactions) as JSON
- **Consent Management:** Track user consent for data processing; require explicit opt-in
- **Purpose Limitation:** Only access card data for legitimate business purposes (log purpose in audit)
- **Breach Notification:** Must notify users within 72 hours of data breach (implement breach detection)

### Decimal Precision Requirements
- **Library:** Use decimal.js for ALL monetary calculations (amounts, limits, balances, fees)
- **Validation:** Zod schemas must validate max 2 decimal places: `z.string().regex(/^\d+\.\d{0,2}$/)`
- **Rounding:** Implement banker's rounding (round half to even): `Decimal.set({ rounding: 6 })`
- **Storage:** Store as PostgreSQL NUMERIC type or string (NEVER use FLOAT, REAL, or DOUBLE PRECISION)
- **Display:** Format with exactly 2 decimals for API responses: `.toFixed(2)`
- **Calculations:** Chain operations: `new Decimal(amount).plus(fee).times(rate).toFixed(2)`

### Idempotency Requirements
- **Keys:** UUID v4 in `Idempotency-Key` header for POST/PATCH/DELETE operations
- **Deduplication Window:** Store idempotency results for 24 hours (Redis or database table)
- **Response Caching:** Return identical response (status code, body) for duplicate keys
- **Atomic Storage:** Store operation result and idempotency key in single database transaction
- **Key Format:** `Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000` (UUID v4)

### Audit Logging Requirements
- **Immutability:** Append-only audit_logs table (no UPDATE or DELETE operations ever)
- **Hash Chaining:** Each record hash = SHA-256(previous_hash + current_record_data); first record hash = SHA-256(record_data)
- **Required Fields:** user_id, action (CREATE_CARD, UPDATE_LIMITS, etc.), resource_type, resource_id, ip_address, timestamp, before_state (JSON), after_state (JSON), hash
- **Retention:** Minimum 7 years per SOX compliance; store in separate high-durability storage
- **Searchability:** Index on user_id, action, resource_id, timestamp for audit queries
- **Verification:** Implement verifyAuditChain() function to detect tampering

### Input Validation Requirements
- **Zod Schemas:** Define schemas for all request bodies (CreateCardSchema, UpdateLimitsSchema, UpdateStateSchema)
- **Luhn Algorithm:** Validate card PAN checksum: `z.string().length(16).refine(luhnCheck, 'Invalid card number')`
- **Spending Limits:** Validate range 0.01 to 1,000,000.00 with max 2 decimals
- **Expiry Date:** Validate future date: `z.string().refine(isFutureDate, 'Expiry must be future date')`
- **State Transitions:** Validate allowed transitions (ACTIVE ↔ FROZEN, any → CLOSED, never CLOSED → any)
- **Sanitization:** Strip HTML/SQL injection attempts from all string inputs

### Error Handling Requirements
- **Production Mode:** NEVER expose stack traces in production (NODE_ENV=production check)
- **Consistent Format:** All error responses: `{ error: { code: 'CARD_NOT_FOUND', message: 'Card not found', details: {} } }`
- **HTTP Status Codes:** 400 (validation), 401 (auth), 403 (forbidden), 404 (not found), 409 (conflict), 429 (rate limit), 500 (server error)
- **Internal Logging:** Log full error with stack trace to Winston (level: error)
- **Custom Error Classes:** ValidationError, NotFoundError, UnauthorizedError, RateLimitError, IdempotencyConflictError
- **Error Middleware:** Global error handler at end of Express middleware chain

### Rate Limiting Requirements
- **Redis-Based:** Use Redis for distributed rate limiting across multiple server instances
- **Per-User Limits:** 100 requests/minute per authenticated user (key: `rate:user:{userId}:min`)
- **Global Limits:** 1,000 requests/minute across all users (key: `rate:global:min`)
- **Velocity Checks:** Maximum 5 card creations per user per 24 hours (key: `rate:user:{userId}:cards:24h`)
- **Sliding Window:** Use sliding window counters for accurate rate limiting
- **Response Headers:** Return `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **429 Response:** Include `Retry-After` header with seconds until reset

### Database Security Requirements
- **Row-Level Encryption:** Encrypt card PAN at application layer before INSERT (database layer encryption as defense-in-depth)
- **Isolation Level:** Use SERIALIZABLE transaction isolation for card operations to prevent race conditions
- **Least Privilege:** Database user has SELECT/INSERT/UPDATE on specific tables only (no DROP, TRUNCATE, ALTER)
- **Parameterized Queries:** Prisma ORM only; NEVER allow raw SQL queries from user input
- **Connection Pooling:** Maximum 20 connections per application instance
- **Backup Encryption:** Daily encrypted backups to separate storage with 7-year retention

### Authentication & Authorization
- **JWT RS256:** Use RSA asymmetric signing (public/private key pair), not HMAC (shared secret)
- **Token Expiry:** 1-hour access token expiry; implement refresh token rotation
- **Secret Rotation:** Rotate signing keys every 90 days with key versioning
- **Claims:** Include `user_id`, `email`, `iat` (issued at), `exp` (expiry)
- **Authorization:** Middleware validates user owns the card before any operation
- **Rate Limiting:** Auth failures count toward rate limit to prevent brute force

### Testing Requirements
- **Coverage Targets:** >85% overall, >90% for services, >80% for controllers
- **Unit Tests (70%):** Test services, utilities, validation logic in isolation with mocks
- **Integration Tests (25%):** Test full API endpoints with real test database (reset between tests)
- **Security Tests (5%):** Test SQL injection, XSS, auth bypass, idempotency violations, rate limit bypass
- **Test Database:** Separate PostgreSQL instance for testing; auto-reset with Prisma migrations
- **Mocking:** Mock external services (Redis, encryption in unit tests) using Jest mocks

### Performance Requirements
- **API Response Time:** <200ms p95 for read operations (GET), <500ms p95 for write operations (POST/PATCH)
- **Database Indexes:** Create indexes on: card_token (unique), user_id, created_at, state
- **Pagination:** Maximum 50 items per page; require pagination for transaction history
- **Caching:** Redis cache for frequently accessed card data (5-minute TTL)
- **Connection Pooling:** Prisma connection pool size: 20 connections
- **Query Optimization:** Use Prisma select to fetch only required fields; avoid N+1 queries

### Security Best Practices
- **Secrets Management:** All secrets (DB password, JWT key, encryption key) in environment variables; use .env.example template
- **Password Hashing:** bcrypt with 12 rounds for user passwords
- **Helmet.js:** Use helmet middleware for security headers (CSP, HSTS, XSS protection)
- **CORS:** Restrict CORS to specific origins; never use `Access-Control-Allow-Origin: *` in production
- **Input Sanitization:** Sanitize all user inputs to prevent XSS attacks
- **Dependency Scanning:** Run `npm audit` weekly; update dependencies with security patches

---

## Context

### Beginning Context (Starting State)

Before implementation begins, the following resources and conditions exist:

1. **Empty Node.js Project:** Initialized repository with package.json, but no application code
2. **PostgreSQL Database:** PostgreSQL 15+ server running and accessible with credentials
3. **Development Tools:** npm/yarn package manager installed, Node.js 20+ runtime available
4. **Git Repository:** Git initialized with .gitignore for node_modules, .env files
5. **Environment Template:** .env.example file exists with required environment variable keys (DB_URL, JWT_SECRET, ENCRYPTION_KEY)
6. **TypeScript Configuration:** tsconfig.json configured with strict mode enabled

### Ending Context (Final State)

After successful implementation, the following deliverables and system state will exist:

1. **Complete RESTful API:** Six fully functional card operation endpoints (create, retrieve, freeze/unfreeze, update limits, transactions, close)
2. **Database Schema:** Prisma schema defining users, cards, transactions, audit_logs, idempotency_keys tables with relationships and indexes
3. **Encryption System:** AES-256-GCM encryption service for card PAN data with secure key management
4. **Comprehensive Test Suite:** >85% code coverage with unit, integration, and security tests (150+ tests)
5. **API Documentation:** OpenAPI 3.1 specification with Swagger UI at /api/docs endpoint
6. **Audit Logging System:** Immutable audit trail with cryptographic hash chaining for all operations
7. **GDPR Compliance:** Data export and anonymization endpoints with 7-year retention policy
8. **Fraud Prevention:** Multi-tier rate limiting with Redis and velocity checks for card creation
9. **Docker Configuration:** docker-compose.yml for local development (PostgreSQL + Redis + App)
10. **CI/CD Pipeline:** GitHub Actions workflow for testing, linting, security scanning, and deployment
11. **Security Scanning:** Integration with Snyk/Dependabot for vulnerability detection
12. **Deployment Documentation:** README with setup instructions, API usage examples, and deployment guide

---

## Low-Level Tasks

### Task 1: Project Setup and Dependencies

**Prompt:**
Initialize a new Node.js TypeScript project for a virtual card lifecycle management API. Install and configure Express.js 4.x, Prisma 5+, Zod 3+, decimal.js, Winston 3+, and Jest 29+ with Supertest. Configure TypeScript in strict mode with appropriate compiler options. Set up ESLint with TypeScript rules, Prettier for code formatting, and Jest for testing. Create .env.example with required environment variables (DATABASE_URL, JWT_SECRET, ENCRYPTION_KEY, REDIS_URL, NODE_ENV, PORT).

**Files to CREATE:**
- `package.json` - Project dependencies and scripts
- `tsconfig.json` - TypeScript compiler configuration with strict mode
- `.eslintrc.json` - ESLint configuration for TypeScript
- `.prettierrc` - Prettier code formatting rules
- `jest.config.js` - Jest testing configuration
- `.env.example` - Environment variable template
- `.gitignore` - Ignore node_modules, .env, dist, coverage

**Functions to CREATE:**
- N/A (configuration files only)

**Details:**
- Node.js version: 20+
- TypeScript strict mode: `strict: true, noImplicitAny: true, strictNullChecks: true`
- Express version: 4.x
- Prisma version: 5+
- Zod version: 3+
- decimal.js: latest
- Winston version: 3+
- Jest version: 29+
- Include dev dependencies: @types/node, @types/express, @types/jest, ts-node, ts-jest, supertest
- Scripts: `dev`, `build`, `test`, `test:coverage`, `lint`, `format`

---

### Task 2: Database Schema Design with Prisma

**Prompt:**
Create a Prisma schema for the virtual card lifecycle management system. Define five models: User (id, email, name, created_at, updated_at), Card (id, user_id, encrypted_pan, card_token, state enum, daily_limit, monthly_limit, created_at, updated_at), Transaction (id, card_id, amount, merchant, status, created_at), AuditLog (id, user_id, action, resource_type, resource_id, ip_address, before_state JSON, after_state JSON, hash, created_at), and IdempotencyKey (id, key, response, expires_at, created_at). Define enums for CardState (CREATED, ACTIVE, FROZEN, CLOSED) and AuditAction (CREATE_CARD, UPDATE_LIMITS, FREEZE_CARD, etc.). Add indexes on card_token (unique), user_id, created_at for performance. Use PostgreSQL NUMERIC type for monetary values.

**Files to CREATE:**
- `prisma/schema.prisma` - Complete database schema

**Functions to CREATE:**
- N/A (schema definition only)

**Details:**
- Database provider: PostgreSQL
- Card state enum: CREATED, ACTIVE, FROZEN, CLOSED (default: CREATED)
- Monetary fields: Use Decimal type (maps to PostgreSQL NUMERIC)
- Indexes: @@unique([card_token]), @@index([user_id]), @@index([created_at])
- Relationships: User 1:N Cards, Card 1:N Transactions, User 1:N AuditLogs
- encrypted_pan field: Store as String (AES-256-GCM ciphertext with IV)
- hash field in AuditLog: SHA-256 hash for chain integrity
- idempotency_keys: TTL handled at application layer (24 hours)

---

### Task 3: Card Encryption Service with AES-256-GCM

**Prompt:**
Create a CardEncryptionService class with methods to encrypt and decrypt card PAN data using AES-256-GCM encryption. Implement encryptPAN(pan: string): EncryptedData that generates a random 16-byte IV, encrypts the PAN with AES-256-GCM, and returns IV + auth tag + ciphertext concatenated. Implement decryptPAN(encryptedData: string): string that extracts IV, auth tag, and ciphertext, then decrypts using the same algorithm. Implement generateToken(): string that creates a unique UUID v4 card token. Implement maskPAN(pan: string): string that returns only the last 4 digits (e.g., "****0366"). Use a 32-byte encryption key from environment variable ENCRYPTION_KEY. NEVER store CVV data.

**Files to CREATE:**
- `src/services/card-encryption.service.ts` - Encryption service implementation
- `src/types/encryption.types.ts` - TypeScript interfaces for EncryptedData

**Functions to CREATE:**
- `encryptPAN(pan: string): EncryptedData` - Encrypts PAN with AES-256-GCM
- `decryptPAN(encryptedData: string): string` - Decrypts PAN
- `generateToken(): string` - Generates UUID v4 card token
- `maskPAN(pan: string): string` - Returns last 4 digits (****0366)

**Details:**
- Use Node.js crypto module: `crypto.createCipheriv('aes-256-gcm', key, iv)`
- Encryption key: 32 bytes from process.env.ENCRYPTION_KEY (base64 encoded)
- IV: 16 bytes random per encryption using `crypto.randomBytes(16)`
- Auth tag: 16 bytes from cipher.getAuthTag()
- Storage format: base64(IV + authTag + ciphertext)
- NEVER store CVV per PCI-DSS requirements
- Validate PAN length (16 digits) before encryption

---

### Task 4: Idempotent Card Creation Endpoint

**Prompt:**
Create POST /api/v1/cards endpoint with idempotency support. The endpoint should: (1) Extract idempotency key from Idempotency-Key header, (2) Check if key exists in database (24h window), return cached response if found, (3) Validate request body using Zod schema (CreateCardSchema: PAN 16 digits, daily_limit 0.01-1000000 with max 2 decimals, monthly_limit 0.01-10000000), (4) Encrypt PAN using CardEncryptionService, (5) Generate unique card_token, (6) Create card record and audit log entry in a single Prisma transaction, (7) Store idempotency key with response (24h expiry), (8) Return 201 Created with masked card data (card_token, last4, limits, state). Use decimal.js for limit validation.

**Files to CREATE:**
- `src/routes/cards.routes.ts` - Express router for card endpoints
- `src/controllers/cards.controller.ts` - Controller handling request/response
- `src/services/cards.service.ts` - Business logic for card operations
- `src/schemas/card.schemas.ts` - Zod validation schemas
- `src/middleware/idempotency.middleware.ts` - Idempotency checking middleware

**Functions to CREATE:**
- `createCard(userId: string, cardData: CreateCardDto, idempotencyKey: string): Promise<CardResponse>` - Main creation logic
- `checkIdempotency(key: string): Promise<CachedResponse | null>` - Check for duplicate key
- `storeIdempotencyResult(key: string, response: any, ttl: number): Promise<void>` - Cache result

**Details:**
- Route: POST /api/v1/cards
- Headers: Authorization: Bearer {jwt}, Idempotency-Key: {uuid}
- Request body: { pan: string, daily_limit: string, monthly_limit: string, expiry: string }
- Validation: Zod schema with decimal.js for limits (max 2 decimals, range 0.01-1000000)
- Idempotency window: 24 hours (86400 seconds)
- Transaction: Prisma.$transaction([createCard, createAuditLog, storeIdempotency])
- Response: 201 Created with { card_token, last4, daily_limit, monthly_limit, state, created_at }
- Error handling: 400 (validation), 409 (idempotency conflict), 500 (server error)

---

### Task 5: Card Retrieval Endpoint with Audit Logging

**Prompt:**
Create GET /api/v1/cards/:cardToken endpoint that retrieves card details. The endpoint should: (1) Validate user owns the card (user_id matches JWT claim), (2) Fetch card from database by card_token, (3) Decrypt PAN using CardEncryptionService, (4) Mask PAN to last 4 digits, (5) Log READ action to audit_logs table with IP address and timestamp, (6) Return 200 OK with masked card details (card_token, last4, daily_limit, monthly_limit, state, created_at, updated_at). NEVER return full PAN or CVV in response. If card not found or user doesn't own it, return 404 Not Found.

**Files to UPDATE:**
- `src/routes/cards.routes.ts` - Add GET route
- `src/controllers/cards.controller.ts` - Add getCardByToken controller
- `src/services/cards.service.ts` - Add retrieval business logic
- `src/services/audit.service.ts` - Create audit logging service

**Functions to CREATE:**
- `getCardByToken(userId: string, cardToken: string): Promise<CardResponse>` - Retrieve and mask card
- `logAudit(auditData: AuditLogDto): Promise<void>` - Log to audit_logs table

**Details:**
- Route: GET /api/v1/cards/:cardToken
- Authorization: Verify user_id from JWT matches card.user_id
- Audit action: READ_CARD
- Response fields: card_token, last4, daily_limit, monthly_limit, state, created_at, updated_at
- NEVER include: encrypted_pan, full PAN, CVV
- Error codes: 401 (unauthorized), 403 (forbidden - wrong user), 404 (not found)
- Audit log fields: user_id, action: 'READ_CARD', resource_type: 'CARD', resource_id: card.id, ip_address, timestamp

---

### Task 6: Card State Management (Freeze/Unfreeze) Endpoint

**Prompt:**
Create PATCH /api/v1/cards/:cardToken/state endpoint to freeze/unfreeze cards. The endpoint should: (1) Validate user owns the card, (2) Validate state transition is allowed (ACTIVE ↔ FROZEN, any state → CLOSED, never CLOSED → any), (3) Fetch current card state, (4) Update card state and create audit log in single transaction, (5) Log before_state and after_state in audit, (6) Return 200 OK with updated card details. Use optimistic locking to prevent race conditions. If invalid transition requested (e.g., CLOSED → ACTIVE), return 400 Bad Request with error message.

**Files to UPDATE:**
- `src/routes/cards.routes.ts` - Add PATCH /cards/:cardToken/state route
- `src/controllers/cards.controller.ts` - Add updateCardState controller
- `src/services/cards.service.ts` - Add state transition business logic

**Functions to CREATE:**
- `updateCardState(userId: string, cardToken: string, newState: CardState): Promise<CardResponse>` - Update state with validation
- `validateStateTransition(currentState: CardState, newState: CardState): boolean` - Validate allowed transitions

**Details:**
- Route: PATCH /api/v1/cards/:cardToken/state
- Request body: { state: 'ACTIVE' | 'FROZEN' | 'CLOSED' }
- Allowed transitions: CREATED→ACTIVE, ACTIVE↔FROZEN, any→CLOSED
- Forbidden transitions: CLOSED→any (permanent state)
- Transaction: Prisma.$transaction([updateCard, createAuditLog])
- Audit log: before_state: { state: 'ACTIVE' }, after_state: { state: 'FROZEN' }
- Optimistic locking: Use Prisma where: { card_token, version } and increment version
- Response: 200 OK with updated card, 400 (invalid transition), 404 (not found)

---

### Task 7: Spending Limits Update Endpoint

**Prompt:**
Create PATCH /api/v1/cards/:cardToken/limits endpoint to update daily and monthly spending limits. The endpoint should: (1) Validate user owns the card, (2) Validate decimal precision using Zod (max 2 decimals, range 0.01-1,000,000.00), (3) Use decimal.js to ensure precision for monetary values, (4) Update limits and create audit log atomically in transaction, (5) Log old limits and new limits in before_state/after_state, (6) Return 200 OK with updated card. Store limits as PostgreSQL NUMERIC or string type (never FLOAT). Validate monthly_limit >= daily_limit.

**Files to UPDATE:**
- `src/routes/cards.routes.ts` - Add PATCH /cards/:cardToken/limits route
- `src/controllers/cards.controller.ts` - Add updateSpendingLimits controller
- `src/services/cards.service.ts` - Add limits update business logic
- `src/schemas/card.schemas.ts` - Add UpdateLimitsSchema

**Functions to CREATE:**
- `updateSpendingLimits(userId: string, cardToken: string, limits: UpdateLimitsDto): Promise<CardResponse>` - Update limits
- `validateLimits(dailyLimit: Decimal, monthlyLimit: Decimal): boolean` - Validate monthly >= daily

**Details:**
- Route: PATCH /api/v1/cards/:cardToken/limits
- Request body: { daily_limit: "500.00", monthly_limit: "5000.00" }
- Validation: Zod schema with decimal.js refinements (max 2 decimals, range 0.01-1000000)
- Business rule: monthly_limit >= daily_limit (return 400 if violated)
- Storage: Store as string or PostgreSQL NUMERIC type
- Transaction: Update card limits + create audit log
- Audit: before_state: { daily_limit: "100.00", monthly_limit: "1000.00" }, after_state: { daily_limit: "500.00", monthly_limit: "5000.00" }
- Response: 200 OK with updated card, 400 (validation error), 404 (not found)

---

### Task 8: Card Transaction History Endpoint

**Prompt:**
Create GET /api/v1/cards/:cardToken/transactions endpoint to retrieve card transaction history with filtering and pagination. Support query parameters: date_from, date_to (ISO 8601 dates), amount_min, amount_max (decimal strings), merchant (partial match), page (default 1), limit (max 50, default 20). Use decimal.js for amount range comparisons. Return transaction list with metadata: transaction_id, amount, merchant, status, timestamp. Include pagination metadata: total, page, limit, total_pages. Ensure user can only access transactions for their own cards.

**Files to UPDATE:**
- `src/routes/cards.routes.ts` - Add GET /cards/:cardToken/transactions route
- `src/controllers/cards.controller.ts` - Add getCardTransactions controller
- `src/services/cards.service.ts` - Add transaction retrieval logic
- `src/services/transaction.service.ts` - Create transaction service (NEW FILE)

**Functions to CREATE:**
- `getCardTransactions(userId: string, cardToken: string, filters: TransactionFilters, pagination: Pagination): Promise<PaginatedTransactions>` - Fetch transactions
- `buildTransactionQuery(filters: TransactionFilters): Prisma.TransactionWhereInput` - Build Prisma where clause

**Details:**
- Route: GET /api/v1/cards/:cardToken/transactions?date_from=2026-01-01&amount_min=10.00&page=1&limit=20
- Query params: date_from, date_to, amount_min, amount_max, merchant, page, limit
- Pagination: Max 50 items per page, default 20
- Filtering: Use Prisma where with AND conditions for multiple filters
- Amount comparison: Use decimal.js: `new Decimal(amount).gte(amount_min).lte(amount_max)`
- Response: { data: [...transactions], meta: { total, page, limit, total_pages } }
- Authorization: Verify card belongs to user before fetching transactions
- Error codes: 403 (forbidden), 404 (card not found), 400 (invalid query params)

---

### Task 9: Audit Logging Middleware with Hash Chaining

**Prompt:**
Create audit logging middleware that automatically logs all API requests to the immutable audit_logs table. Capture: user_id (from JWT), action (derived from HTTP method + endpoint), resource_type, resource_id, ip_address (from request), timestamp, before_state (existing data), after_state (new data), and hash (SHA-256 chain). Implement cryptographic hash chaining: hash = SHA-256(previous_hash + current_record_data) where previous_hash is the hash of the last audit record; first record hash = SHA-256(record_data). Create verifyAuditChain() function to detect tampering by recomputing and comparing hashes. NEVER allow UPDATE or DELETE operations on audit_logs table.

**Files to CREATE:**
- `src/middleware/audit.middleware.ts` - Express middleware for audit logging
- `src/services/audit.service.ts` - Audit logging business logic
- `src/utils/hash.utils.ts` - SHA-256 hashing utilities

**Functions to CREATE:**
- `auditMiddleware(req, res, next)` - Express middleware that intercepts requests
- `logAudit(auditData: AuditLogDto): Promise<void>` - Create audit log entry
- `getLastAuditHash(): Promise<string | null>` - Fetch previous hash for chaining
- `computeHash(previousHash: string, recordData: object): string` - SHA-256(prev + current)
- `verifyAuditChain(): Promise<boolean>` - Verify entire chain integrity

**Details:**
- Middleware placement: After authentication, before route handlers
- Hash algorithm: SHA-256 using Node.js crypto.createHash('sha256')
- Hash input: Concatenate previous_hash + JSON.stringify(current_record)
- First record: hash = SHA-256(JSON.stringify(record)) with no previous hash
- Storage: Append-only table; use Prisma without UPDATE/DELETE queries
- Action mapping: POST /cards → CREATE_CARD, PATCH /cards/:id/state → UPDATE_STATE
- IP address: Extract from req.ip or req.headers['x-forwarded-for']
- Async logging: Don't block request; log asynchronously with try/catch

---

### Task 10: GDPR Data Export and Deletion Endpoints

**Prompt:**
Create two GDPR compliance endpoints. First, GET /api/v1/users/me/data that exports all user data as JSON: user profile (name, email, created_at), cards (card_token, last4, limits, state - masked PAN), and transactions (transaction_id, amount, merchant, timestamp). Second, DELETE /api/v1/users/me that anonymizes user PII while preserving audit logs. Anonymization replaces user.name with "DELETED_USER", user.email with "deleted_{user_id}@deleted.local", and sets deleted_at timestamp. NEVER hard-delete records; retain all data for 7-year compliance period. Keep all audit logs intact (immutable). Return 200 OK with success message.

**Files to CREATE:**
- `src/routes/gdpr.routes.ts` - GDPR endpoint routes
- `src/controllers/gdpr.controller.ts` - GDPR request handlers
- `src/services/gdpr.service.ts` - GDPR business logic

**Functions to CREATE:**
- `exportUserData(userId: string): Promise<UserDataExport>` - Export complete user data
- `anonymizeUserData(userId: string): Promise<void>` - Anonymize PII while preserving audit
- `buildUserExport(user, cards, transactions): UserDataExport` - Assemble export JSON

**Details:**
- Export route: GET /api/v1/users/me/data
- Deletion route: DELETE /api/v1/users/me
- Export format: { user: {...}, cards: [...], transactions: [...] }
- Card masking: Return card_token and last4 only (never full PAN)
- Anonymization: user.name = "DELETED_USER", user.email = "deleted_{uuid}@deleted.local", user.deleted_at = NOW()
- Audit preservation: NEVER delete or modify audit_logs table
- 7-year retention: Implement scheduled job (cron) to purge records after 7 years
- Response: 200 OK with { message: "User data anonymized successfully" }
- Authorization: User can only export/delete their own data (verify JWT user_id)

---

### Task 11: Rate Limiting and Fraud Prevention Middleware

**Prompt:**
Create multi-tier rate limiting middleware using Redis. Implement three rate limit rules: (1) 100 requests/minute per authenticated user, (2) 1,000 requests/minute global limit across all users, (3) Maximum 5 card creations per user per 24 hours (velocity check). Use Redis sliding window counters with TTL. Return 429 Too Many Requests with Retry-After header when limit exceeded. Include rate limit info in response headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset. Use Redis keys: rate:user:{userId}:min, rate:global:min, rate:user:{userId}:cards:24h.

**Files to CREATE:**
- `src/middleware/rate-limit.middleware.ts` - Rate limiting middleware
- `src/services/rate-limit.service.ts` - Redis-based rate limit logic
- `src/config/redis.config.ts` - Redis client configuration

**Functions to CREATE:**
- `rateLimitMiddleware(req, res, next)` - Express middleware for rate limiting
- `checkRateLimit(userId: string, action: string): Promise<RateLimitResult>` - Check all limits
- `incrementCounter(key: string, ttl: number): Promise<number>` - Increment Redis counter
- `getRemainingRequests(key: string, limit: number): Promise<number>` - Calculate remaining

**Details:**
- Redis keys: rate:user:{userId}:min (TTL 60s), rate:global:min (TTL 60s), rate:user:{userId}:cards:24h (TTL 86400s)
- Per-user limit: 100 req/min (increment user key, check < 100)
- Global limit: 1000 req/min (increment global key, check < 1000)
- Card creation velocity: Max 5 cards/24h (increment cards key on POST /cards, check < 5)
- Sliding window: Use Redis INCR + EXPIRE for simple sliding window
- 429 Response: { error: { code: 'RATE_LIMIT_EXCEEDED', message: 'Too many requests' } }
- Headers: X-RateLimit-Limit: 100, X-RateLimit-Remaining: 23, X-RateLimit-Reset: 1640995200
- Retry-After: Include seconds until reset in header

---

### Task 12: Input Validation with Zod Schemas

**Prompt:**
Create comprehensive Zod validation schemas for all request bodies and a validation middleware. Define CreateCardSchema with: pan (16 digits, Luhn algorithm valid), daily_limit (decimal string, max 2 decimals, range 0.01-1000000), monthly_limit (decimal string, max 2 decimals, range 0.01-10000000), expiry (future date YYYY-MM). Define UpdateLimitsSchema with daily_limit and monthly_limit validation. Define UpdateStateSchema with state enum validation. Create validation middleware that parses request body with Zod schema and returns 400 Bad Request with detailed error messages if validation fails. Use custom Zod refinements for Luhn algorithm and decimal.js precision checks.

**Files to CREATE:**
- `src/schemas/card.schemas.ts` - Zod validation schemas for card operations
- `src/middleware/validation.middleware.ts` - Express validation middleware
- `src/utils/luhn.utils.ts` - Luhn algorithm implementation

**Functions to CREATE:**
- `CreateCardSchema` - Zod schema for POST /cards request body
- `UpdateLimitsSchema` - Zod schema for PATCH /cards/:id/limits
- `UpdateStateSchema` - Zod schema for PATCH /cards/:id/state
- `validateRequest(schema: ZodSchema)` - Express middleware factory
- `luhnCheck(cardNumber: string): boolean` - Luhn algorithm validation

**Details:**
- PAN validation: `z.string().length(16).regex(/^\d{16}$/).refine(luhnCheck, 'Invalid card number')`
- Limit validation: `z.string().regex(/^\d+\.\d{0,2}$/).refine(val => new Decimal(val).gte(0.01).lte(1000000), 'Invalid amount')`
- Expiry validation: `z.string().regex(/^\d{4}-\d{2}$/).refine(isValidExpiry, 'Expiry must be future date')`
- State validation: `z.enum(['CREATED', 'ACTIVE', 'FROZEN', 'CLOSED'])`
- Middleware: `const validate = (schema) => (req, res, next) => { const result = schema.safeParse(req.body); ... }`
- Error response: 400 with { error: { code: 'VALIDATION_ERROR', message: 'Validation failed', details: zodError.issues } }
- Luhn algorithm: Sum odd digits + sum even digits * 2 (if >= 10, subtract 9); total % 10 === 0

---

### Task 13: Global Error Handler Middleware

**Prompt:**
Create a global error handling middleware with custom error classes for different error types. Define custom error classes: ValidationError (400), NotFoundError (404), UnauthorizedError (401), ForbiddenError (403), RateLimitError (429), IdempotencyConflictError (409), and InternalServerError (500). Each error class should include statusCode, code, and message. Create global error handler middleware that: (1) Logs full error with stack trace to Winston at appropriate level (4xx = warn, 5xx = error), (2) Returns sanitized JSON response to client (NEVER include stack trace in production), (3) Uses consistent error format: { error: { code, message, details } }. Place at end of Express middleware chain.

**Files to CREATE:**
- `src/middleware/error-handler.middleware.ts` - Global error handler middleware
- `src/errors/custom-errors.ts` - Custom error class definitions
- `src/config/logger.config.ts` - Winston logger configuration

**Functions to CREATE:**
- `errorHandler(err, req, res, next)` - Express error handling middleware
- `ValidationError` class - Extends Error with statusCode 400
- `NotFoundError` class - Extends Error with statusCode 404
- `UnauthorizedError` class - Extends Error with statusCode 401
- `RateLimitError` class - Extends Error with statusCode 429
- `IdempotencyConflictError` class - Extends Error with statusCode 409

**Details:**
- Error class structure: `class ValidationError extends Error { statusCode = 400; code = 'VALIDATION_ERROR'; }`
- Logging levels: 4xx errors → logger.warn(), 5xx errors → logger.error()
- Production mode: NEVER include err.stack in response (check NODE_ENV === 'production')
- Response format: `{ error: { code: 'CARD_NOT_FOUND', message: 'Card not found', details: {} } }`
- HTTP status codes: Use err.statusCode if available, default to 500
- Winston config: Console transport for dev, File transport for production with rotation
- Middleware placement: app.use(errorHandler) at the very end after all routes

---

### Task 14: Comprehensive Test Suite with Jest and Supertest

**Prompt:**
Create a comprehensive test suite covering unit tests, integration tests, and security tests. Unit tests (70% of tests): Test services (CardEncryptionService, CardsService, AuditService) and utilities (luhn, hash) in isolation using Jest mocks. Integration tests (25% of tests): Test all API endpoints with real test database using Supertest; reset database between tests with Prisma migrations. Security tests (5% of tests): Test SQL injection attempts, XSS payloads, authentication bypass, idempotency violations, and rate limit bypass. Target >85% overall code coverage, >90% for services. Configure Jest with ts-jest for TypeScript support. Create test database separate from development database.

**Files to CREATE:**
- `src/services/__tests__/card-encryption.service.test.ts` - Unit tests for encryption
- `src/services/__tests__/cards.service.test.ts` - Unit tests for card business logic
- `src/services/__tests__/audit.service.test.ts` - Unit tests for audit logging
- `tests/integration/cards.integration.test.ts` - Integration tests for card endpoints
- `tests/integration/gdpr.integration.test.ts` - Integration tests for GDPR endpoints
- `tests/security/injection.security.test.ts` - Security tests for SQL injection, XSS
- `tests/security/auth.security.test.ts` - Security tests for auth bypass
- `jest.config.js` - Jest configuration with coverage thresholds

**Functions to CREATE:**
- N/A (test functions only)

**Details:**
- Test framework: Jest 29+ with ts-jest preset
- Integration testing: Supertest for HTTP requests to Express app
- Test database: Separate PostgreSQL instance; reset with `beforeEach(() => resetTestDB())`
- Mocking: Jest mocks for external services (Redis, encryption in unit tests)
- Coverage targets: >85% statements, >85% branches, >85% functions, >90% services
- Test organization: Unit tests in src/**/__tests__, integration in tests/integration/, security in tests/security/
- Test scripts: `npm test` (all tests), `npm run test:unit`, `npm run test:integration`, `npm run test:coverage`
- Security test cases: SQL injection payloads, XSS scripts, auth bypass attempts, invalid idempotency keys

---

### Task 15: API Documentation with OpenAPI 3.1

**Prompt:**
Create comprehensive API documentation using OpenAPI 3.1 specification. Document all six card operation endpoints: POST /api/v1/cards (create), GET /api/v1/cards/:cardToken (retrieve), PATCH /api/v1/cards/:cardToken/state (freeze/unfreeze), PATCH /api/v1/cards/:cardToken/limits (update limits), GET /api/v1/cards/:cardToken/transactions (history), and GDPR endpoints. Include request/response schemas with examples, authentication requirements (JWT Bearer), error response formats, rate limiting information, and idempotency behavior. Serve documentation using Swagger UI at /api/docs endpoint. Include security scheme definition for JWT Bearer token.

**Files to CREATE:**
- `docs/openapi.yaml` - OpenAPI 3.1 specification document
- `src/routes/swagger.routes.ts` - Express route to serve Swagger UI
- `src/config/swagger.config.ts` - Swagger UI configuration

**Functions to CREATE:**
- N/A (configuration and YAML only)

**Details:**
- OpenAPI version: 3.1.0
- Servers: Development (http://localhost:3000), Production (https://api.example.com)
- Security scheme: JWT Bearer token (type: http, scheme: bearer, bearerFormat: JWT)
- Components/schemas: Define Card, Transaction, Error, CreateCardRequest, UpdateLimitsRequest, etc.
- Examples: Include example requests and responses for each endpoint
- Error responses: Document 400, 401, 403, 404, 429, 500 for each endpoint
- Rate limits: Document in endpoint descriptions (100 req/min per user, etc.)
- Idempotency: Document Idempotency-Key header requirement for POST/PATCH
- Swagger UI: Use swagger-ui-express package, serve at /api/docs
- Tags: Group endpoints by resource (Cards, Transactions, GDPR)

---

## End of Specification

This specification provides a complete, implementable blueprint for building a PCI-DSS and GDPR compliant virtual card lifecycle management API. Each low-level task contains executable prompts suitable for AI-driven development, with explicit file paths, function signatures, and detailed requirements. The specification emphasizes security, compliance, and financial precision through industry best practices.

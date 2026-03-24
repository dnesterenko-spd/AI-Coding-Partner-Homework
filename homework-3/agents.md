# AI Agent Guidelines - Virtual Card Lifecycle Management System

## Project Overview

This is a **Virtual Card Lifecycle Management API** built with Node.js, TypeScript, and PostgreSQL. The system is designed to be **PCI-DSS and GDPR compliant**, handling sensitive financial data (card PANs, spending limits, transactions) with enterprise-grade security, comprehensive audit logging, and regulatory compliance. The API supports six core card operations: create, retrieve, freeze/unfreeze, update limits, view transactions, and close.

**Core Principles:**
- **Security First:** PCI-DSS compliance with encryption, tokenization, and access controls
- **Financial Precision:** decimal.js for ALL monetary calculations (never use Number)
- **Audit Everything:** Immutable audit trail with cryptographic hash chaining
- **GDPR Compliance:** Right to deletion/export, 7-year retention, data minimization
- **Idempotency:** All state-changing operations support idempotency keys
- **Rate Limiting:** Multi-tier rate limiting for fraud prevention

---

## Tech Stack

### Core Technologies
| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Runtime | Node.js LTS | 20+ | JavaScript runtime environment |
| Language | TypeScript | 5+ | Type-safe JavaScript with strict mode |
| Framework | Express.js | 4.x | Lightweight web framework for REST APIs |
| Database | PostgreSQL | 15+ | ACID-compliant relational database |
| ORM | Prisma | 5+ | Type-safe database client with migrations |

### Financial & Validation Libraries
| Library | Version | Purpose |
|---------|---------|---------|
| decimal.js | latest | Arbitrary-precision decimal arithmetic (required for money) |
| Zod | 3+ | TypeScript-first schema validation with refinements |

### Security & Compliance
| Library | Version | Purpose |
|---------|---------|---------|
| Node.js crypto | built-in | AES-256-GCM encryption for PAN data |
| bcrypt | latest | Password hashing with configurable rounds |
| helmet | latest | Security headers (CSP, HSTS, XSS protection) |
| jsonwebtoken | latest | JWT RS256 token generation/verification |

### Logging & Monitoring
| Library | Version | Purpose |
|---------|---------|---------|
| Winston | 3+ | Structured logging with multiple transports |

### Caching & Rate Limiting
| Library | Version | Purpose |
|---------|---------|---------|
| Redis (ioredis) | latest | Distributed rate limiting and session caching |

### Testing
| Library | Version | Purpose |
|---------|---------|---------|
| Jest | 29+ | Unit and integration testing framework |
| Supertest | latest | HTTP assertion library for API testing |
| @faker-js/faker | latest | Generate test data (cards, users, transactions) |

### Development Tools
| Tool | Version | Purpose |
|------|---------|---------|
| ESLint | latest | TypeScript linting with recommended rules |
| Prettier | latest | Code formatting with consistent style |
| ts-node | latest | TypeScript execution for development |
| nodemon | latest | Auto-restart on file changes |

---

## Banking Domain Rules

### Rule 1: Decimal Precision for Monetary Values (CRITICAL)

**Problem:** JavaScript's Number type uses IEEE 754 floating-point arithmetic, which causes precision errors in financial calculations.

**Wrong Approach (NEVER USE):**
```typescript
// ❌ WRONG - Floating point errors
const dailyLimit = 0.1;
const fee = 0.2;
const total = dailyLimit + fee; // 0.30000000000004 (incorrect!)

const price = 0.9;
const quantity = 3;
const total2 = price * quantity; // 2.6999999999999997 (incorrect!)

// Storing in database
const limit = 1234.56; // Type: number (WRONG)
await prisma.card.create({
  data: { daily_limit: limit } // Will cause precision loss
});
```

**Correct Approach (ALWAYS USE):**
```typescript
// ✅ RIGHT - Use decimal.js for ALL money operations
import Decimal from 'decimal.js';

// Configure banker's rounding (round half to even)
Decimal.set({ rounding: 6 }); // ROUND_HALF_EVEN

// Calculations
const dailyLimit = new Decimal('0.1');
const fee = new Decimal('0.2');
const total = dailyLimit.plus(fee).toFixed(2); // "0.30" (correct!)

const price = new Decimal('0.9');
const quantity = 3;
const total2 = price.times(quantity).toFixed(2); // "2.70" (correct!)

// Complex calculation chain
const spending = new Decimal('1234.56');
const cashback = new Decimal('0.015'); // 1.5%
const fee = new Decimal('2.50');
const netAmount = spending.times(cashback).minus(fee).toFixed(2); // "16.02"

// Storing in database (as string)
await prisma.card.create({
  data: {
    daily_limit: '1234.56', // Store as string
    monthly_limit: new Decimal(5000).toFixed(2) // "5000.00"
  }
});

// Validation with Zod
const LimitSchema = z.object({
  daily_limit: z.string()
    .regex(/^\d+\.\d{2}$/, 'Must have exactly 2 decimal places')
    .refine(val => {
      const amount = new Decimal(val);
      return amount.gte('0.01') && amount.lte('1000000.00');
    }, 'Amount must be between 0.01 and 1,000,000.00')
});
```

**Key Points:**
- ALWAYS use `new Decimal(value)` for money, NEVER use `Number`
- Store monetary values as `string` or PostgreSQL `NUMERIC` type
- Use `.toFixed(2)` for exactly 2 decimal places in output
- Chain operations: `.plus()`, `.minus()`, `.times()`, `.dividedBy()`
- Compare with `.gte()`, `.lte()`, `.eq()` methods, not `>`, `<`, `===`

---

### Rule 2: Idempotency for State-Changing Operations (CRITICAL)

**Problem:** Network retries can cause duplicate operations (double card creation, double charges).

**Implementation Pattern:**
```typescript
// ✅ Idempotency pattern for card creation
async function createCard(
  userId: string,
  cardData: CreateCardDto,
  idempotencyKey: string
): Promise<CardResponse> {

  // Step 1: Check if idempotency key exists (24h window)
  const existingResult = await prisma.idempotencyKey.findUnique({
    where: { key: idempotencyKey },
    include: { response: true }
  });

  // Step 2: If key exists and hasn't expired, return cached response
  if (existingResult && existingResult.expires_at > new Date()) {
    return JSON.parse(existingResult.response); // Return exact same response
  }

  // Step 3: Process operation (validate, encrypt, create)
  const encryptedPAN = await encryptionService.encryptPAN(cardData.pan);
  const cardToken = encryptionService.generateToken();

  // Step 4: Store result and idempotency key atomically
  const result = await prisma.$transaction(async (tx) => {
    // Create card
    const card = await tx.card.create({
      data: {
        user_id: userId,
        encrypted_pan: encryptedPAN,
        card_token: cardToken,
        daily_limit: cardData.daily_limit,
        monthly_limit: cardData.monthly_limit,
        state: 'CREATED'
      }
    });

    // Create audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'CREATE_CARD',
        resource_type: 'CARD',
        resource_id: card.id,
        after_state: { card_token: cardToken, state: 'CREATED' },
        hash: computeHash(prevHash, currentData)
      }
    });

    // Store idempotency key (24h expiry)
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000);
    await tx.idempotencyKey.create({
      data: {
        key: idempotencyKey,
        response: JSON.stringify({ card_token: cardToken, last4: cardData.pan.slice(-4) }),
        expires_at: expiresAt
      }
    });

    return { card_token: cardToken, last4: cardData.pan.slice(-4), state: card.state };
  });

  return result;
}
```

**Key Points:**
- Idempotency key: UUID v4 in `Idempotency-Key` header
- Deduplication window: 24 hours (86400 seconds)
- Return EXACT same response for duplicate keys (status code + body)
- Store operation + idempotency key in single transaction
- Clean up expired keys with scheduled job

---

### Rule 3: Atomic Transactions for Data Consistency (CRITICAL)

**Problem:** Partial failures can leave system in inconsistent state (card created but audit log missing).

**Implementation Pattern:**
```typescript
// ✅ Atomic transaction pattern with Prisma
async function updateCardState(
  userId: string,
  cardToken: string,
  newState: CardState
): Promise<CardResponse> {

  return await prisma.$transaction(async (tx) => {
    // Step 1: Fetch current card with optimistic locking
    const card = await tx.card.findUnique({
      where: { card_token: cardToken }
    });

    if (!card) {
      throw new NotFoundError('Card not found');
    }

    if (card.user_id !== userId) {
      throw new ForbiddenError('Not authorized');
    }

    // Step 2: Validate state transition
    if (!isValidTransition(card.state, newState)) {
      throw new ValidationError(`Invalid transition: ${card.state} → ${newState}`);
    }

    // Step 3: Update card state
    const updatedCard = await tx.card.update({
      where: { card_token: cardToken },
      data: { state: newState, updated_at: new Date() }
    });

    // Step 4: Create audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'UPDATE_STATE',
        resource_type: 'CARD',
        resource_id: card.id,
        before_state: { state: card.state },
        after_state: { state: newState },
        hash: computeHash(prevHash, currentData)
      }
    });

    // Transaction succeeds: both operations committed
    // Transaction fails: both operations rolled back
    return {
      card_token: updatedCard.card_token,
      state: updatedCard.state
    };
  }, {
    isolationLevel: 'Serializable' // Highest isolation for financial operations
  });
}
```

**Key Points:**
- Use `prisma.$transaction()` for multi-step operations
- All operations succeed together or fail together (atomicity)
- Use `Serializable` isolation level for financial operations
- Optimistic locking: Check version/timestamp before update
- NEVER split related operations across multiple database calls

---

### Rule 4: PCI-DSS Compliance (CRITICAL)

**What NEVER to Do:**
```typescript
// ❌ WRONG - PCI-DSS violations
// 1. Storing CVV (NEVER EVER)
await prisma.card.create({
  data: {
    pan: '4532015112830366',
    cvv: '123' // ❌ PCI-DSS violation! NEVER store CVV
  }
});

// 2. Logging sensitive data
logger.info(`Card created: PAN=${pan}, CVV=${cvv}`); // ❌ PAN/CVV in logs

// 3. Returning full PAN in API response
return { pan: '4532015112830366' }; // ❌ Full PAN exposed

// 4. Storing PAN unencrypted
await prisma.card.create({
  data: { pan: '4532015112830366' } // ❌ Plain text PAN
});
```

**Correct Implementation:**
```typescript
// ✅ RIGHT - PCI-DSS compliant
// 1. NEVER store CVV (accept for transaction, discard immediately)
async function processTransaction(cardToken: string, cvv: string) {
  // Use CVV for transaction authorization
  const authorized = await authorizeCVV(cvv);
  // CVV is NEVER saved to database
  // ...
}

// 2. Encrypt PAN before storage
const encryptedPAN = encryptionService.encryptPAN('4532015112830366');
const cardToken = generateToken(); // UUID v4
await prisma.card.create({
  data: {
    encrypted_pan: encryptedPAN, // AES-256-GCM encrypted
    card_token: cardToken // Use this for references
  }
});

// 3. Always return masked PAN (last 4 only)
function maskPAN(pan: string): string {
  return `****${pan.slice(-4)}`; // "****0366"
}

return { card_token: cardToken, last4: pan.slice(-4) }; // ✅

// 4. Log access to card data (audit)
await auditLog.create({
  data: {
    user_id: userId,
    action: 'READ_CARD',
    resource_type: 'CARD',
    resource_id: cardId,
    ip_address: req.ip
  }
});
```

**PCI-DSS Checklist:**
- [ ] Encrypt all card PAN data at rest (AES-256-GCM)
- [ ] Use tokenization (card_token) for external references
- [ ] NEVER store CVV/CVC/CID data (even encrypted)
- [ ] Use TLS 1.3 for all data in transit
- [ ] Log all access to card data with user_id, action, timestamp
- [ ] Return masked PAN only (last 4 digits)
- [ ] Separate card data storage from other system components

---

### Rule 5: Comprehensive Audit Logging (CRITICAL)

**What to Log:**
```typescript
// ✅ Complete audit log structure
interface AuditLogEntry {
  user_id: string;           // WHO performed the action
  action: string;            // WHAT action (CREATE_CARD, UPDATE_LIMITS, etc.)
  resource_type: string;     // Resource type (CARD, TRANSACTION, USER)
  resource_id: string;       // Specific resource ID
  ip_address: string;        // WHERE (IP address or 'x-forwarded-for')
  timestamp: Date;           // WHEN (UTC timestamp)
  before_state: object;      // State BEFORE change (for updates)
  after_state: object;       // State AFTER change
  hash: string;              // SHA-256 hash for chain integrity
}

// Example: Log card creation
await auditLog.create({
  data: {
    user_id: '550e8400-e29b-41d4-a716-446655440000',
    action: 'CREATE_CARD',
    resource_type: 'CARD',
    resource_id: card.id,
    ip_address: '192.168.1.100',
    timestamp: new Date(),
    before_state: null, // No previous state for creation
    after_state: {
      card_token: cardToken,
      state: 'CREATED',
      daily_limit: '1000.00',
      monthly_limit: '10000.00'
    },
    hash: computeHash(previousHash, currentData)
  }
});

// Example: Log limit update
await auditLog.create({
  data: {
    user_id: userId,
    action: 'UPDATE_LIMITS',
    resource_type: 'CARD',
    resource_id: card.id,
    before_state: {
      daily_limit: '1000.00',
      monthly_limit: '10000.00'
    },
    after_state: {
      daily_limit: '2000.00',
      monthly_limit: '20000.00'
    },
    hash: computeHash(previousHash, currentData)
  }
});
```

**Hash Chaining for Tamper Detection:**
```typescript
// ✅ Cryptographic hash chaining
import crypto from 'crypto';

function computeHash(previousHash: string | null, recordData: object): string {
  const data = JSON.stringify(recordData);

  if (previousHash === null) {
    // First record: hash = SHA-256(data)
    return crypto.createHash('sha256').update(data).digest('hex');
  } else {
    // Subsequent records: hash = SHA-256(previous_hash + data)
    return crypto.createHash('sha256')
      .update(previousHash + data)
      .digest('hex');
  }
}

// Verify audit chain integrity
async function verifyAuditChain(): Promise<boolean> {
  const logs = await prisma.auditLog.findMany({
    orderBy: { created_at: 'asc' }
  });

  let previousHash: string | null = null;

  for (const log of logs) {
    const expectedHash = computeHash(previousHash, {
      user_id: log.user_id,
      action: log.action,
      resource_type: log.resource_type,
      resource_id: log.resource_id,
      before_state: log.before_state,
      after_state: log.after_state
    });

    if (log.hash !== expectedHash) {
      return false; // Tampering detected!
    }

    previousHash = log.hash;
  }

  return true; // Chain is valid
}
```

**Key Points:**
- Log ALL state-changing operations (create, update, delete)
- Append-only table (NEVER UPDATE or DELETE audit logs)
- Hash chaining prevents tampering
- 7-year minimum retention per SOX compliance
- Include before/after state for updates

---

### Rule 6: GDPR Compliance - Anonymization over Deletion (HIGH)

**Problem:** GDPR requires right to deletion, but financial regulations require 7-year retention.

**Solution: Anonymize PII while preserving audit trails**
```typescript
// ❌ WRONG - Hard delete (violates audit retention)
async function deleteUser(userId: string) {
  await prisma.user.delete({ where: { id: userId } }); // ❌ Loses all data
  await prisma.auditLog.deleteMany({ where: { user_id: userId } }); // ❌ Loses audit
}

// ✅ RIGHT - Anonymize PII, keep audit
async function anonymizeUserData(userId: string): Promise<void> {
  await prisma.$transaction(async (tx) => {
    // Anonymize user PII
    await tx.user.update({
      where: { id: userId },
      data: {
        name: 'DELETED_USER',
        email: `deleted_${userId}@deleted.local`,
        phone: null,
        deleted_at: new Date()
      }
    });

    // Anonymize card data (keep token for audit)
    await tx.card.updateMany({
      where: { user_id: userId },
      data: {
        encrypted_pan: null, // Remove encrypted PAN
        state: 'CLOSED'
      }
    });

    // KEEP audit logs (immutable, required for compliance)
    // Audit logs reference user_id but user is anonymized

    // Create audit entry for deletion request
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'ANONYMIZE_USER',
        resource_type: 'USER',
        resource_id: userId,
        after_state: { deleted_at: new Date() }
      }
    });
  });
}

// Data export for GDPR
async function exportUserData(userId: string): Promise<UserDataExport> {
  const user = await prisma.user.findUnique({ where: { id: userId } });
  const cards = await prisma.card.findMany({ where: { user_id: userId } });
  const transactions = await prisma.transaction.findMany({
    where: { card: { user_id: userId } }
  });

  return {
    user: {
      name: user.name,
      email: user.email,
      created_at: user.created_at
    },
    cards: cards.map(card => ({
      card_token: card.card_token,
      last4: card.encrypted_pan ? decryptPAN(card.encrypted_pan).slice(-4) : null,
      daily_limit: card.daily_limit,
      monthly_limit: card.monthly_limit,
      state: card.state,
      created_at: card.created_at
    })),
    transactions: transactions.map(tx => ({
      transaction_id: tx.id,
      amount: tx.amount,
      merchant: tx.merchant,
      timestamp: tx.created_at
    }))
  };
}
```

**GDPR Checklist:**
- [ ] Right to deletion: Anonymize PII (don't hard delete)
- [ ] Right to export: Return all user data as JSON
- [ ] 7-year retention: Automated purge after 7 years
- [ ] Purpose limitation: Log purpose for each data access
- [ ] Consent management: Track user consent in database
- [ ] Breach notification: Notify within 72 hours of breach

---

### Rule 7: Rate Limiting with Redis for Fraud Prevention (HIGH)

**Implementation Pattern:**
```typescript
// ✅ Multi-tier rate limiting with Redis
import Redis from 'ioredis';

const redis = new Redis(process.env.REDIS_URL);

async function checkRateLimit(
  userId: string,
  action: string
): Promise<RateLimitResult> {

  const now = Math.floor(Date.now() / 1000); // Unix timestamp
  const minute = Math.floor(now / 60);

  // Rule 1: Per-user limit (100 req/min)
  const userKey = `rate:user:${userId}:${minute}`;
  const userCount = await redis.incr(userKey);
  await redis.expire(userKey, 60); // TTL 60 seconds

  if (userCount > 100) {
    throw new RateLimitError('User rate limit exceeded', 60 - (now % 60));
  }

  // Rule 2: Global limit (1000 req/min)
  const globalKey = `rate:global:${minute}`;
  const globalCount = await redis.incr(globalKey);
  await redis.expire(globalKey, 60);

  if (globalCount > 1000) {
    throw new RateLimitError('Global rate limit exceeded', 60 - (now % 60));
  }

  // Rule 3: Card creation velocity (max 5 cards per 24h)
  if (action === 'CREATE_CARD') {
    const day = Math.floor(now / 86400);
    const velocityKey = `rate:user:${userId}:cards:${day}`;
    const cardCount = await redis.incr(velocityKey);
    await redis.expire(velocityKey, 86400); // TTL 24 hours

    if (cardCount > 5) {
      throw new RateLimitError('Card creation limit exceeded (max 5/day)', 86400 - (now % 86400));
    }
  }

  return {
    allowed: true,
    remaining: 100 - userCount,
    resetAt: (minute + 1) * 60 // Next minute
  };
}

// Middleware
function rateLimitMiddleware(req, res, next) {
  const userId = req.user.id;
  const action = req.method === 'POST' && req.path === '/cards' ? 'CREATE_CARD' : 'API_REQUEST';

  try {
    const result = await checkRateLimit(userId, action);

    // Add rate limit headers
    res.setHeader('X-RateLimit-Limit', '100');
    res.setHeader('X-RateLimit-Remaining', result.remaining);
    res.setHeader('X-RateLimit-Reset', result.resetAt);

    next();
  } catch (error) {
    if (error instanceof RateLimitError) {
      res.setHeader('Retry-After', error.retryAfter);
      return res.status(429).json({
        error: {
          code: 'RATE_LIMIT_EXCEEDED',
          message: error.message
        }
      });
    }
    next(error);
  }
}
```

**Key Points:**
- Use Redis for distributed rate limiting (scales horizontally)
- Per-user limit: 100 requests/minute
- Global limit: 1000 requests/minute
- Velocity checks: Max 5 card creations per 24 hours
- Return 429 with `Retry-After` header

---

## Code Style Standards

### Naming Conventions
- **Files/Directories:** kebab-case (`card-encryption.service.ts`, `rate-limit.middleware.ts`)
- **Classes:** PascalCase (`CardEncryptionService`, `CardsController`)
- **Functions/Variables:** camelCase (`encryptPAN`, `cardToken`, `userId`)
- **Constants:** UPPER_SNAKE_CASE (`MAX_SPENDING_LIMIT`, `REDIS_TTL`)
- **Database Columns:** snake_case (`card_token`, `user_id`, `created_at`)
- **Interfaces/Types:** PascalCase with descriptive names (`CreateCardDto`, `CardResponse`)

### File Organization
```
src/
├── config/          # Configuration files (database, logger, redis)
├── controllers/     # Request handlers (thin layer)
├── services/        # Business logic (thick layer)
├── repositories/    # Data access layer (optional)
├── middleware/      # Express middleware (auth, validation, rate limit)
├── routes/          # Express route definitions
├── schemas/         # Zod validation schemas
├── types/           # TypeScript type definitions
├── utils/           # Utility functions (hash, luhn, etc.)
├── errors/          # Custom error classes
└── __tests__/       # Unit tests co-located with source
```

### Error Handling Pattern
```typescript
// Custom error class hierarchy
class AppError extends Error {
  constructor(public statusCode: number, public code: string, message: string) {
    super(message);
  }
}

class ValidationError extends AppError {
  constructor(message: string) {
    super(400, 'VALIDATION_ERROR', message);
  }
}

class NotFoundError extends AppError {
  constructor(message: string) {
    super(404, 'NOT_FOUND', message);
  }
}

// Usage in service
if (!card) {
  throw new NotFoundError('Card not found');
}

// Global error handler catches and formats
app.use((err, req, res, next) => {
  logger.error(err);
  res.status(err.statusCode || 500).json({
    error: {
      code: err.code || 'INTERNAL_ERROR',
      message: err.message
    }
  });
});
```

---

## Testing Requirements

### Coverage Targets
- **Overall Coverage:** >85% (statements, branches, functions)
- **Services:** >90% coverage (business logic is critical)
- **Controllers:** >80% coverage
- **Utilities:** >95% coverage (pure functions are easy to test)

### Test Distribution
- **Unit Tests (70%):** Services, utilities, validation functions
- **Integration Tests (25%):** API endpoints with real test database
- **Security Tests (5%):** SQL injection, XSS, auth bypass, rate limit bypass

### Test Organization
```
src/
└── services/
    ├── card-encryption.service.ts
    └── __tests__/
        └── card-encryption.service.test.ts  # Co-located unit tests

tests/
├── integration/
│   ├── cards.integration.test.ts
│   └── gdpr.integration.test.ts
└── security/
    ├── injection.security.test.ts
    └── auth.security.test.ts
```

---

## Security Constraints

### Authentication
- **Algorithm:** JWT RS256 (asymmetric signing with public/private key)
- **Expiry:** 1-hour access token, 7-day refresh token
- **Key Rotation:** Rotate signing keys every 90 days
- **Claims:** Include `user_id`, `email`, `iat`, `exp`, `jti` (JWT ID for revocation)

### Secrets Management
- **Environment Variables:** ALL secrets in `.env` file (DATABASE_URL, JWT_SECRET, ENCRYPTION_KEY)
- **Never Commit:** Add `.env` to `.gitignore`
- **Template:** Provide `.env.example` with dummy values
- **Production:** Use secrets manager (AWS Secrets Manager, HashiCorp Vault)

### Input Validation
- **Zod Schemas:** Define schemas for ALL request bodies
- **Luhn Check:** Validate card number checksum
- **Whitelist:** Only allow expected fields (strip unknown properties)
- **Sanitization:** Strip HTML/SQL from string inputs

### Database Security
- **Least Privilege:** Database user has SELECT/INSERT/UPDATE only (no DROP/TRUNCATE)
- **Parameterized Queries:** Prisma ORM only (prevents SQL injection)
- **Row-Level Encryption:** Encrypt PAN at application layer
- **Connection Pooling:** Max 20 connections per instance

---

## Performance Considerations

### Database Optimization
- **Indexes:** Create indexes on `card_token` (unique), `user_id`, `created_at`, `state`
- **Pagination:** Always paginate list endpoints (max 50 items per page)
- **Select Specific Fields:** Use Prisma `select` to fetch only needed fields
- **Avoid N+1 Queries:** Use Prisma `include` for relationships

### Caching Strategy
- **Redis Cache:** Cache frequently accessed card data (TTL: 5 minutes)
- **Cache Invalidation:** Invalidate on UPDATE/DELETE operations
- **Cache Keys:** `card:{cardToken}`, `user:{userId}:cards`

### API Response Time Targets
- **Read Operations (GET):** <200ms p95
- **Write Operations (POST/PATCH):** <500ms p95
- **Transaction History:** <300ms p95 with pagination

---

## Compliance Requirements

### PCI-DSS Checklist (7 Items)
- [ ] Encrypt card PAN at rest (AES-256-GCM)
- [ ] Use tokenization for card references (card_token)
- [ ] NEVER store CVV/CVC/CID data
- [ ] Use TLS 1.3 for data in transit
- [ ] Log all access to card data
- [ ] Isolate card data storage (separate database schema)
- [ ] Annual penetration testing and quarterly vulnerability scans

### GDPR Checklist (6 Items)
- [ ] Obtain explicit consent for data processing
- [ ] Right to deletion (anonymization endpoint)
- [ ] Right to export (data export endpoint as JSON)
- [ ] 7-year retention with automatic purging
- [ ] Purpose limitation (log purpose for data access)
- [ ] Breach notification within 72 hours

### Audit Trail Requirements (5 Items)
- [ ] Immutable audit logs (append-only, no UPDATE/DELETE)
- [ ] 7-year minimum retention per SOX compliance
- [ ] Cryptographic hash chaining for tamper detection
- [ ] Searchable by user_id, action, resource_id, timestamp
- [ ] Complete before/after state for all changes

---

## Common Patterns to Follow

### Service Layer Pattern
```typescript
// Separation of concerns: Controller handles HTTP, Service handles business logic

// Controller (thin)
class CardsController {
  async createCard(req: Request, res: Response, next: NextFunction) {
    try {
      const userId = req.user.id;
      const cardData = req.body;
      const idempotencyKey = req.headers['idempotency-key'];

      const card = await cardsService.createCard(userId, cardData, idempotencyKey);

      res.status(201).json(card);
    } catch (error) {
      next(error);
    }
  }
}

// Service (thick - business logic)
class CardsService {
  async createCard(userId: string, cardData: CreateCardDto, idempotencyKey: string) {
    // Check idempotency
    // Validate data
    // Encrypt PAN
    // Create card + audit in transaction
    // Return response
  }
}
```

### Repository Pattern (Optional)
```typescript
// Data access abstraction (optional, Prisma is already an abstraction)
class CardRepository {
  async findByToken(cardToken: string): Promise<Card | null> {
    return await prisma.card.findUnique({
      where: { card_token: cardToken }
    });
  }

  async create(data: CreateCardData): Promise<Card> {
    return await prisma.card.create({ data });
  }
}
```

### Middleware Chain Pattern
```typescript
// Ordered middleware chain
app.use(helmet()); // Security headers
app.use(express.json()); // Parse JSON
app.use(authMiddleware); // Authentication (JWT verification)
app.use(rateLimitMiddleware); // Rate limiting
app.post('/cards', validateRequest(CreateCardSchema), cardsController.createCard); // Validation → Handler
app.use(auditMiddleware); // Audit logging
app.use(errorHandler); // Global error handler (must be last)
```

---

## Anti-Patterns to Avoid

### ❌ Financial Calculations with Number
```typescript
// WRONG - Never use Number for money
const limit = 1234.56; // Type: number (precision loss)
const total = limit * 0.015; // Floating point error
```

### ❌ Storing PAN Unencrypted
```typescript
// WRONG - PCI-DSS violation
await prisma.card.create({
  data: { pan: '4532015112830366' } // Plain text
});
```

### ❌ Exposing Stack Traces in Production
```typescript
// WRONG - Security vulnerability
app.use((err, req, res, next) => {
  res.status(500).json({ error: err.stack }); // ❌ Stack trace leak
});
```

### ❌ Raw SQL Queries
```typescript
// WRONG - SQL injection risk
const userId = req.params.userId;
await prisma.$queryRaw(`SELECT * FROM cards WHERE user_id = ${userId}`); // ❌ Injection
```

### ❌ Hardcoded Secrets
```typescript
// WRONG - Security vulnerability
const JWT_SECRET = 'my-secret-key-12345'; // ❌ In source code
const ENCRYPTION_KEY = 'hardcoded-key'; // ❌ Committed to git
```

---

## AI Collaboration Guidelines

### Code Generation Workflow
1. **Understand Requirement:** Read specification task carefully
2. **Check Existing Code:** Search for similar implementations to reuse patterns
3. **Generate Code:** Write TypeScript with strict types
4. **Add Tests:** Write unit tests for new functions
5. **Document:** Add JSDoc comments for public APIs

### Code Review Checklist (10 Items)
- [ ] Uses decimal.js for ALL monetary calculations
- [ ] PAN encryption before storage, never CVV
- [ ] Zod validation schemas for all inputs
- [ ] Prisma transactions for multi-step operations
- [ ] Audit logging for all state changes
- [ ] Custom error classes with proper status codes
- [ ] Unit tests with >90% coverage for services
- [ ] No secrets hardcoded in code
- [ ] TypeScript strict mode with no `any` types
- [ ] Performance: Indexes, pagination, caching

### Documentation Requirements
- **JSDoc:** Required for all public APIs (controllers, services)
- **Inline Comments:** Only for complex logic (not obvious code)
- **README:** Update with new endpoints, setup instructions
- **OpenAPI:** Add new endpoints to `docs/openapi.yaml`

---

## Summary

This document provides comprehensive AI guidelines for building a secure, compliant virtual card lifecycle management API. The key principles are:

1. **Security First:** PCI-DSS compliance with encryption, tokenization, and audit logging
2. **Financial Precision:** Always use decimal.js for money (never Number)
3. **Idempotency:** All state-changing operations must support idempotency keys
4. **GDPR Compliance:** Anonymization over deletion, 7-year retention
5. **Rate Limiting:** Multi-tier fraud prevention with Redis
6. **Atomic Transactions:** Use Prisma transactions for data consistency
7. **Comprehensive Audit:** Immutable logs with cryptographic hash chaining

Follow these guidelines rigorously to ensure the system meets enterprise-grade security, compliance, and reliability standards.

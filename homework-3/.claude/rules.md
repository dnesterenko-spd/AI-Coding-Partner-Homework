# Claude Code Rules - Virtual Card Lifecycle Management API

## Project Type

**FinTech API - Virtual Card Lifecycle (PCI-DSS + GDPR Compliant)**

This is an enterprise-grade financial application handling sensitive card data with strict security, compliance, and precision requirements.

---

## Critical Constraints

### ❌ NEVER

1. **❌ Use Number type for monetary values**
   - JavaScript Number uses floating-point (0.1 + 0.2 = 0.30000000000004)
   - ALWAYS use decimal.js: `new Decimal('0.1').plus('0.2').toFixed(2)` → "0.30"

2. **❌ Store card PAN unencrypted**
   - PCI-DSS violation
   - ALWAYS encrypt with AES-256-GCM before storage: `encryptPAN(pan)`

3. **❌ Store CVV/CVC/CID data (even encrypted)**
   - PCI-DSS SAQ A requirement violation
   - CVV is ONLY used for transaction authorization, then immediately discarded

4. **❌ Log sensitive data (PAN, CVV, passwords)**
   - Security and compliance violation
   - Log card_token instead of PAN
   - Never log: `logger.info(User created card: ${pan})`

5. **❌ Use raw SQL queries**
   - SQL injection risk
   - ALWAYS use Prisma ORM: `prisma.card.findMany()` not `$queryRaw()`

6. **❌ Return stack traces in production**
   - Security vulnerability (exposes internal system details)
   - Sanitize errors: `{ error: { code, message } }` without `err.stack`

7. **❌ Hard-code secrets in source code**
   - Security vulnerability
   - ALWAYS use environment variables: `process.env.ENCRYPTION_KEY`

8. **❌ Modify audit logs (UPDATE/DELETE)**
   - Compliance violation (audit trail must be immutable)
   - Audit logs are append-only: only INSERT operations

### ✅ ALWAYS

1. **✅ Use Decimal.js for all money calculations**
   - Financial precision is non-negotiable
   - `new Decimal(amount).plus(fee).toFixed(2)`

2. **✅ Encrypt PAN with AES-256-GCM before storage**
   - PCI-DSS compliance requirement
   - Store encrypted_pan and card_token, never plain PAN

3. **✅ Return masked PAN (last 4 digits only) in responses**
   - API responses: `{ card_token, last4: pan.slice(-4) }`
   - Never expose full PAN in any API response

4. **✅ Log all state changes to audit table**
   - Every CREATE/UPDATE/DELETE must have audit entry
   - Include before_state and after_state for all changes

5. **✅ Validate inputs with Zod schemas**
   - Type-safe validation with custom refinements
   - `CreateCardSchema.parse(req.body)`

6. **✅ Use Prisma transactions for multi-step operations**
   - Atomic operations: all succeed or all fail
   - `await prisma.$transaction([op1, op2])`

7. **✅ Check idempotency keys for state-changing operations**
   - POST/PATCH/DELETE operations require idempotency-key header
   - 24-hour deduplication window

8. **✅ Sanitize error responses (remove stack traces)**
   - Production errors: `{ error: { code, message } }`
   - Internal logging: `logger.error(err)` with full stack

---

## Naming Conventions

### Files and Directories
- **Format:** kebab-case
- **Examples:**
  - `card-encryption.service.ts`
  - `rate-limit.middleware.ts`
  - `cards.controller.ts`
  - `audit.service.ts`

### Code Naming

**Classes:**
- **Format:** PascalCase
- **Examples:**
  - `CardEncryptionService`
  - `CardsController`
  - `ValidationError`

**Functions and Variables:**
- **Format:** camelCase
- **Examples:**
  - `encryptPAN()`
  - `generateToken()`
  - `cardToken`
  - `userId`

**Constants:**
- **Format:** UPPER_SNAKE_CASE
- **Examples:**
  - `MAX_SPENDING_LIMIT`
  - `IDEMPOTENCY_TTL`
  - `REDIS_KEY_PREFIX`

### Database Columns
- **Format:** snake_case
- **Examples:**
  - `card_token`
  - `user_id`
  - `created_at`
  - `daily_limit`

---

## Financial Calculation Patterns

### ❌ WRONG - Floating Point Arithmetic

```typescript
// WRONG - Precision loss
const dailyLimit = 0.1;
const fee = 0.2;
const total = dailyLimit + fee; // 0.30000000000004 ❌

const price = 0.9;
const quantity = 3;
const revenue = price * quantity; // 2.6999999999999997 ❌

// WRONG - Number type in database
interface Card {
  daily_limit: number; // ❌ Floating point
}
```

### ✅ RIGHT - Decimal.js

```typescript
// RIGHT - Exact precision
import Decimal from 'decimal.js';

// Configure banker's rounding (round half to even)
Decimal.set({ rounding: 6 }); // ROUND_HALF_EVEN

const dailyLimit = new Decimal('0.1');
const fee = new Decimal('0.2');
const total = dailyLimit.plus(fee).toFixed(2); // "0.30" ✅

const price = new Decimal('0.9');
const quantity = 3;
const revenue = price.times(quantity).toFixed(2); // "2.70" ✅

// Complex calculation chain
const spending = new Decimal('1234.56');
const cashbackRate = new Decimal('0.015'); // 1.5%
const processingFee = new Decimal('2.50');

const cashback = spending.times(cashbackRate); // 18.5184
const netCashback = cashback.minus(processingFee).toFixed(2); // "16.02" ✅

// Comparisons
const limit = new Decimal('1000.00');
const amount = new Decimal('999.99');

if (amount.lte(limit)) { // Use .lte() not <=
  console.log('Within limit');
}

// Storage - as string or NUMERIC
interface Card {
  daily_limit: string; // ✅ Store as string
  monthly_limit: string; // ✅ Store as string
}

await prisma.card.create({
  data: {
    daily_limit: new Decimal(1000).toFixed(2), // "1000.00"
    monthly_limit: '10000.00'
  }
});
```

---

## Security Patterns

### Card PAN Handling Pattern

```typescript
// ✅ Encrypt → Store token → Return masked

// Step 1: Encrypt PAN before storage
const pan = '4532015112830366';
const encryptedPAN = await encryptionService.encryptPAN(pan);
// encryptedPAN = "base64(IV + authTag + ciphertext)"

// Step 2: Generate unique token for external references
const cardToken = generateToken(); // UUID v4

// Step 3: Store encrypted PAN + token
await prisma.card.create({
  data: {
    encrypted_pan: encryptedPAN,
    card_token: cardToken,
    user_id: userId,
    daily_limit: '1000.00'
  }
});

// Step 4: Return masked PAN only
return {
  card_token: cardToken,
  last4: pan.slice(-4), // "0366"
  daily_limit: '1000.00',
  state: 'CREATED'
};

// NEVER return full PAN:
// ❌ return { pan: '4532015112830366' }
// ❌ return { encrypted_pan: encryptedPAN }
```

### Audit Logging Pattern

```typescript
// ✅ Log every state change with before/after

// Before update
const oldCard = await prisma.card.findUnique({
  where: { card_token: cardToken }
});

// Perform update
const newCard = await prisma.card.update({
  where: { card_token: cardToken },
  data: { daily_limit: '2000.00' }
});

// Create audit log
await prisma.auditLog.create({
  data: {
    user_id: userId,
    action: 'UPDATE_LIMITS',
    resource_type: 'CARD',
    resource_id: oldCard.id,
    ip_address: req.ip,
    before_state: {
      daily_limit: oldCard.daily_limit,
      monthly_limit: oldCard.monthly_limit
    },
    after_state: {
      daily_limit: newCard.daily_limit,
      monthly_limit: newCard.monthly_limit
    },
    hash: computeHash(previousHash, currentData)
  }
});

// Hash chaining for tamper detection
function computeHash(previousHash: string | null, recordData: object): string {
  const data = JSON.stringify(recordData);
  const hash = crypto.createHash('sha256');

  if (previousHash === null) {
    // First record: hash = SHA-256(data)
    return hash.update(data).digest('hex');
  } else {
    // Subsequent: hash = SHA-256(prev_hash + data)
    return hash.update(previousHash + data).digest('hex');
  }
}
```

### Idempotency Pattern

```typescript
// ✅ Check → Process → Store result atomically

async function createCard(
  userId: string,
  cardData: CreateCardDto,
  idempotencyKey: string
): Promise<CardResponse> {

  // Step 1: Check if idempotency key exists
  const existing = await prisma.idempotencyKey.findUnique({
    where: { key: idempotencyKey }
  });

  // Step 2: If exists and not expired, return cached result
  if (existing && existing.expires_at > new Date()) {
    return JSON.parse(existing.response); // Return exact same response
  }

  // Step 3: Process operation
  const encryptedPAN = await encryptionService.encryptPAN(cardData.pan);
  const cardToken = generateToken();

  // Step 4: Store result + idempotency key in transaction
  const result = await prisma.$transaction(async (tx) => {
    const card = await tx.card.create({
      data: {
        user_id: userId,
        encrypted_pan: encryptedPAN,
        card_token: cardToken,
        daily_limit: cardData.daily_limit,
        monthly_limit: cardData.monthly_limit
      }
    });

    // Audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'CREATE_CARD',
        resource_id: card.id,
        after_state: { card_token: cardToken }
      }
    });

    // Store idempotency key (24h expiry)
    const response = {
      card_token: cardToken,
      last4: cardData.pan.slice(-4)
    };

    await tx.idempotencyKey.create({
      data: {
        key: idempotencyKey,
        response: JSON.stringify(response),
        expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000)
      }
    });

    return response;
  });

  return result;
}
```

---

## Testing Patterns

### Unit Test Pattern (Arrange-Act-Assert)

```typescript
import { CardEncryptionService } from '../card-encryption.service';

describe('CardEncryptionService', () => {
  let service: CardEncryptionService;

  beforeEach(() => {
    service = new CardEncryptionService();
  });

  it('should encrypt and decrypt PAN correctly', () => {
    // Arrange
    const originalPAN = '4532015112830366';

    // Act
    const encrypted = service.encryptPAN(originalPAN);
    const decrypted = service.decryptPAN(encrypted);

    // Assert
    expect(decrypted).toBe(originalPAN);
    expect(encrypted).not.toContain(originalPAN); // PAN not visible in ciphertext
  });

  it('should mask PAN to last 4 digits', () => {
    // Arrange
    const pan = '4532015112830366';

    // Act
    const masked = service.maskPAN(pan);

    // Assert
    expect(masked).toBe('****0366');
  });

  it('should throw error for invalid PAN length', () => {
    // Arrange
    const invalidPAN = '123456789'; // Too short

    // Act & Assert
    expect(() => service.encryptPAN(invalidPAN)).toThrow('Invalid PAN length');
  });
});
```

### Integration Test Pattern

```typescript
import request from 'supertest';
import { app } from '../app';
import { resetTestDB } from './helpers/db-helper';

describe('POST /api/v1/cards', () => {
  beforeEach(async () => {
    await resetTestDB(); // Reset database before each test
  });

  it('should create card with valid data', async () => {
    // Setup
    const token = generateTestJWT({ user_id: 'test-user-123' });
    const idempotencyKey = 'test-key-' + Date.now();

    const validCardData = {
      pan: '4532015112830366', // Valid Luhn
      daily_limit: '1000.00',
      monthly_limit: '10000.00',
      expiry: '2026-12'
    };

    // Request
    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    // Assert
    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty('card_token');
    expect(response.body.last4).toBe('0366');
    expect(response.body.daily_limit).toBe('1000.00');
    expect(response.body.state).toBe('CREATED');
  });

  it('should return 409 for duplicate idempotency key', async () => {
    const token = generateTestJWT({ user_id: 'test-user-123' });
    const idempotencyKey = 'duplicate-key';

    // First request
    await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    // Second request with same key
    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    expect(response.status).toBe(201); // Returns cached response
    expect(response.body.card_token).toBeDefined(); // Same as first response
  });

  it('should return 400 for invalid PAN (Luhn check fails)', async () => {
    const invalidData = {
      pan: '4532015112830367', // Invalid Luhn
      daily_limit: '1000.00',
      monthly_limit: '10000.00'
    };

    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', 'test-key')
      .send(invalidData);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('VALIDATION_ERROR');
  });
});
```

---

## Error Handling Pattern

### Custom Error Classes

```typescript
// Base error class
class AppError extends Error {
  constructor(
    public statusCode: number,
    public code: string,
    message: string
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

// Specific error types
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

class UnauthorizedError extends AppError {
  constructor(message: string) {
    super(401, 'UNAUTHORIZED', message);
  }
}

class ForbiddenError extends AppError {
  constructor(message: string) {
    super(403, 'FORBIDDEN', message);
  }
}

class RateLimitError extends AppError {
  constructor(message: string, public retryAfter: number) {
    super(429, 'RATE_LIMIT_EXCEEDED', message);
  }
}

class IdempotencyConflictError extends AppError {
  constructor(message: string) {
    super(409, 'IDEMPOTENCY_CONFLICT', message);
  }
}

// Usage in service
async function getCardByToken(userId: string, cardToken: string) {
  const card = await prisma.card.findUnique({
    where: { card_token: cardToken }
  });

  if (!card) {
    throw new NotFoundError('Card not found');
  }

  if (card.user_id !== userId) {
    throw new ForbiddenError('You do not own this card');
  }

  return card;
}
```

### Global Error Handler Middleware

```typescript
// Global error handler (must be last middleware)
function errorHandler(
  err: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Log full error internally
  if (err instanceof AppError && err.statusCode >= 500) {
    logger.error(err); // Include stack trace
  } else if (err instanceof AppError) {
    logger.warn(err.message);
  } else {
    logger.error(err); // Unknown error
  }

  // Determine status code and error code
  const statusCode = err instanceof AppError ? err.statusCode : 500;
  const code = err instanceof AppError ? err.code : 'INTERNAL_ERROR';

  // Sanitize error response (NEVER include stack trace in production)
  const response: any = {
    error: {
      code,
      message: err.message
    }
  };

  // Add retry-after for rate limit errors
  if (err instanceof RateLimitError) {
    res.setHeader('Retry-After', err.retryAfter);
  }

  // NEVER include stack trace in production
  if (process.env.NODE_ENV === 'development') {
    response.error.stack = err.stack; // Only in dev
  }

  res.status(statusCode).json(response);
}

// Register middleware (at the end)
app.use(errorHandler);
```

---

## Database Transaction Pattern

### Atomic Operations with Prisma

```typescript
// ✅ Use transactions for multi-step operations

async function updateCardState(
  userId: string,
  cardToken: string,
  newState: CardState
): Promise<CardResponse> {

  return await prisma.$transaction(async (tx) => {
    // Step 1: Fetch current card
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
      data: {
        state: newState,
        updated_at: new Date()
      }
    });

    // Step 4: Create audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'UPDATE_STATE',
        resource_type: 'CARD',
        resource_id: card.id,
        before_state: { state: card.state },
        after_state: { state: newState }
      }
    });

    // If any step fails, entire transaction is rolled back
    // If all steps succeed, transaction is committed
    return {
      card_token: updatedCard.card_token,
      state: updatedCard.state
    };
  }, {
    isolationLevel: 'Serializable' // Highest isolation for financial data
  });
}

// Allowed state transitions
function isValidTransition(current: CardState, next: CardState): boolean {
  const transitions = {
    CREATED: ['ACTIVE', 'CLOSED'],
    ACTIVE: ['FROZEN', 'CLOSED'],
    FROZEN: ['ACTIVE', 'CLOSED'],
    CLOSED: [] // Terminal state - no transitions allowed
  };

  return transitions[current]?.includes(next) || false;
}
```

---

## Validation Pattern

### Zod Schemas with Custom Refinements

```typescript
import { z } from 'zod';
import Decimal from 'decimal.js';

// Luhn algorithm for card number validation
function luhnCheck(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  let sum = 0;
  let isEven = false;

  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = parseInt(digits[i], 10);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
}

// Card creation schema
const CreateCardSchema = z.object({
  pan: z.string()
    .length(16, 'PAN must be exactly 16 digits')
    .regex(/^\d{16}$/, 'PAN must contain only digits')
    .refine(luhnCheck, 'Invalid card number (Luhn check failed)'),

  daily_limit: z.string()
    .regex(/^\d+\.\d{2}$/, 'Must have exactly 2 decimal places')
    .refine(val => {
      const amount = new Decimal(val);
      return amount.gte('0.01') && amount.lte('1000000.00');
    }, 'Daily limit must be between 0.01 and 1,000,000.00'),

  monthly_limit: z.string()
    .regex(/^\d+\.\d{2}$/, 'Must have exactly 2 decimal places')
    .refine(val => {
      const amount = new Decimal(val);
      return amount.gte('0.01') && amount.lte('10000000.00');
    }, 'Monthly limit must be between 0.01 and 10,000,000.00'),

  expiry: z.string()
    .regex(/^\d{4}-\d{2}$/, 'Expiry format must be YYYY-MM')
    .refine(val => {
      const [year, month] = val.split('-').map(Number);
      const expiryDate = new Date(year, month - 1);
      return expiryDate > new Date();
    }, 'Expiry date must be in the future')
}).refine(data => {
  // Business rule: monthly_limit >= daily_limit
  const daily = new Decimal(data.daily_limit);
  const monthly = new Decimal(data.monthly_limit);
  return monthly.gte(daily);
}, {
  message: 'Monthly limit must be >= daily limit',
  path: ['monthly_limit']
});

// Validation middleware factory
function validateRequest(schema: z.ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    const result = schema.safeParse(req.body);

    if (!result.success) {
      return res.status(400).json({
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Validation failed',
          details: result.error.issues
        }
      });
    }

    // Replace req.body with validated data
    req.body = result.data;
    next();
  };
}

// Usage in route
app.post('/api/v1/cards',
  authMiddleware,
  validateRequest(CreateCardSchema),
  cardsController.createCard
);
```

---

## Code Quality Rules

### TypeScript Strict Mode

```json
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "strictPropertyInitialization": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true
  }
}
```

### Function Length
- **Maximum:** 50 lines per function
- **Rationale:** Easier to understand, test, and maintain
- **Solution:** Extract helper functions for complex logic

### Comments
- **JSDoc:** Required for all public APIs (controllers, services, exported functions)
- **Inline:** Only for complex/non-obvious logic
- **Avoid:** Obvious comments like `// Increment counter`

```typescript
// ✅ Good - JSDoc for public API
/**
 * Creates a new virtual card with encrypted PAN and spending limits.
 *
 * @param userId - The user creating the card
 * @param cardData - Card details (PAN, limits, expiry)
 * @param idempotencyKey - UUID for idempotency (24h dedup window)
 * @returns Masked card details (token, last4, limits, state)
 * @throws ValidationError if data is invalid
 * @throws RateLimitError if user exceeded card creation limit (5/day)
 */
async function createCard(
  userId: string,
  cardData: CreateCardDto,
  idempotencyKey: string
): Promise<CardResponse> {
  // ...
}

// ✅ Good - Complex logic comment
// Luhn algorithm: Sum odd digits + sum even digits * 2 (if >= 10, subtract 9)
// Valid if total % 10 === 0

// ❌ Bad - Obvious comment
const total = a + b; // Add a and b
```

### Dependencies
- **Pin Versions:** Use exact versions in package.json (`"5.2.1"` not `"^5.2.1"`)
- **Security Audit:** Run `npm audit` weekly
- **Update Strategy:** Update dependencies with security patches immediately

---

## Summary

These rules enforce security, compliance, and financial precision for the virtual card lifecycle API:

1. **NEVER use Number for money** - Always use decimal.js
2. **NEVER store PAN unencrypted** - AES-256-GCM required
3. **NEVER store CVV** - PCI-DSS violation
4. **ALWAYS validate with Zod** - Type-safe schemas
5. **ALWAYS use transactions** - Atomic operations
6. **ALWAYS audit state changes** - Immutable logs
7. **ALWAYS check idempotency** - 24h dedup window
8. **ALWAYS sanitize errors** - No stack traces in production

Follow these patterns rigorously for a secure, compliant FinTech application.

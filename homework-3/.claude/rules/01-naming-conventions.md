# Naming Conventions

## Files and Directories

- **Format:** kebab-case
- **Examples:**
  - `card-encryption.service.ts`
  - `rate-limit.middleware.ts`
  - `cards.controller.ts`
  - `audit.service.ts`

---

## Code Naming

### Classes

- **Format:** PascalCase
- **Examples:**
  - `CardEncryptionService`
  - `CardsController`
  - `ValidationError`

### Functions and Variables

- **Format:** camelCase
- **Examples:**
  - `encryptPAN()`
  - `generateToken()`
  - `cardToken`
  - `userId`

### Constants

- **Format:** UPPER_SNAKE_CASE
- **Examples:**
  - `MAX_SPENDING_LIMIT`
  - `IDEMPOTENCY_TTL`
  - `REDIS_KEY_PREFIX`

---

## Database Columns

- **Format:** snake_case
- **Examples:**
  - `card_token`
  - `user_id`
  - `created_at`
  - `daily_limit`

---

## File Organization

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
# Critical Constraints - Virtual Card Lifecycle API

## Project Type

**FinTech API - Virtual Card Lifecycle (PCI-DSS + GDPR Compliant)**

This is an enterprise-grade financial application handling sensitive card data with strict security, compliance, and precision requirements.

---

## ❌ NEVER

### 1. Use Number type for monetary values
- JavaScript Number uses floating-point (0.1 + 0.2 = 0.30000000000004)
- ALWAYS use decimal.js: `new Decimal('0.1').plus('0.2').toFixed(2)` → "0.30"

### 2. Store card PAN unencrypted
- PCI-DSS violation
- ALWAYS encrypt with AES-256-GCM before storage: `encryptPAN(pan)`

### 3. Store CVV/CVC/CID data (even encrypted)
- PCI-DSS SAQ A requirement violation
- CVV is ONLY used for transaction authorization, then immediately discarded

### 4. Log sensitive data (PAN, CVV, passwords)
- Security and compliance violation
- Log card_token instead of PAN
- Never log: `logger.info('User created card: ${pan}')`

### 5. Use raw SQL queries
- SQL injection risk
- ALWAYS use Prisma ORM: `prisma.card.findMany()` not `$queryRaw()`

### 6. Return stack traces in production
- Security vulnerability (exposes internal system details)
- Sanitize errors: `{ error: { code, message } }` without `err.stack`

### 7. Hard-code secrets in source code
- Security vulnerability
- ALWAYS use environment variables: `process.env.ENCRYPTION_KEY`

### 8. Modify audit logs (UPDATE/DELETE)
- Compliance violation (audit trail must be immutable)
- Audit logs are append-only: only INSERT operations

---

## ✅ ALWAYS

### 1. Use Decimal.js for all money calculations
- Financial precision is non-negotiable
- `new Decimal(amount).plus(fee).toFixed(2)`

### 2. Encrypt PAN with AES-256-GCM before storage
- PCI-DSS compliance requirement
- Store encrypted_pan and card_token, never plain PAN

### 3. Return masked PAN (last 4 digits only) in responses
- API responses: `{ card_token, last4: pan.slice(-4) }`
- Never expose full PAN in any API response

### 4. Log all state changes to audit table
- Every CREATE/UPDATE/DELETE must have audit entry
- Include before_state and after_state for all changes

### 5. Validate inputs with Zod schemas
- Type-safe validation with custom refinements
- `CreateCardSchema.parse(req.body)`

### 6. Use Prisma transactions for multi-step operations
- Atomic operations: all succeed or all fail
- `await prisma.$transaction([op1, op2])`

### 7. Check idempotency keys for state-changing operations
- POST/PATCH/DELETE operations require idempotency-key header
- 24-hour deduplication window

### 8. Sanitize error responses (remove stack traces)
- Production errors: `{ error: { code, message } }`
- Internal logging: `logger.error(err)` with full stack

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

### Dependencies
- **Pin Versions:** Use exact versions in package.json (`"5.2.1"` not `"^5.2.1"`)
- **Security Audit:** Run `npm audit` weekly
- **Update Strategy:** Update dependencies with security patches immediately
# Homework 3: Specification-Driven Design

**Student:** Dmytro Nesterenko
**Course:** AI Coding Partner
**Date:** March 2026

---

## Overview

This is a **specification package** for a **Virtual Card Lifecycle Management API** designed for a regulated FinTech environment. The package demonstrates specification-driven design methodology by decomposing complex financial requirements into implementable documentation that AI coding partners can execute.

### Deliverables

This homework includes **four comprehensive documents** (design-only, no implementation):

1. **specification.md** (8-12 pages) - Complete product specification with high-level objectives, mid-level measurable goals, implementation notes, context boundaries, and 15 detailed low-level tasks with executable AI prompts
2. **agents.md** (5-7 pages) - AI guidelines with tech stack details, 7 banking domain rules with code examples, code style standards, testing requirements, security constraints, and PCI-DSS/GDPR/audit trail checklists
3. **.claude/rules/*.md** (8 files, 3-4 pages) - Editor-specific rules organized by topic: critical constraints, naming conventions, financial calculations, security patterns, testing, error handling, database transactions, and validation
4. **README.md** (this file, 4-6 pages) - Rationale, best practices mapping, industry references, design choices, and usage instructions

**Note:** This is a **design-only homework**. No code implementation is required. The focus is on documentation quality, industry best practices demonstration, and clear communication of technical requirements to AI agents.

---

## Feature Selection: Virtual Card Lifecycle Management

### What

A **Virtual Card Lifecycle Management API** that supports six core operations:

1. **Create Virtual Card** - Generate new virtual card with spending limits (daily/monthly)
2. **Retrieve Card Details** - Get card information with masked PAN (last 4 digits only)
3. **Freeze/Unfreeze Card** - Toggle card state between ACTIVE and FROZEN
4. **Update Spending Limits** - Modify daily and monthly spending limits
5. **View Transaction History** - Retrieve card transactions with filtering and pagination
6. **Close Card** - Permanently close card (terminal state)

The system manages card lifecycle states: **CREATED → ACTIVE → FROZEN → CLOSED**

### Rationale: Why Virtual Cards?

| Reason | Description |
|--------|-------------|
| **Industry Relevance** | Virtual cards are core to modern FinTech platforms (Stripe Issuing, Marqeta, Lithic, Privacy.com). Demonstrates understanding of current payment infrastructure and API-first card issuing trends. |
| **Rich Compliance Surface** | Showcases multiple critical compliance frameworks: PCI-DSS (card data security), GDPR (privacy), SOX (audit trails), and financial regulations. Perfect for demonstrating enterprise-grade compliance knowledge. |
| **Clear Lifecycle States** | Natural state machine (CREATED → ACTIVE → FROZEN → CLOSED) demonstrates system evolution from "beginning context" to "ending context" in specification. Lifecycle provides clear boundaries for operations. |
| **Security Depth** | Enables demonstration of advanced security practices: PAN encryption (AES-256-GCM), tokenization, CVV handling (never stored), transaction controls, and comprehensive access logging. |
| **Manageable Scope** | 6 operations with 15 detailed implementation tasks = **optimal breadth + depth** balance. Sufficient complexity to demonstrate enterprise thinking without overwhelming the specification. Focused enough for clear, executable AI prompts. |

### Alternatives Considered

| Alternative | Pros | Cons | Why Not Chosen |
|------------|------|------|----------------|
| **Account-to-Account Transfers** | Common banking operation, clear money flow | Too simple, limited compliance surface (no PCI-DSS), less security depth | Insufficient demonstration of FinTech best practices |
| **Loan Origination System** | Complex workflow, regulatory requirements | Too workflow-heavy, less technical depth, requires extensive business logic modeling | Complexity in wrong areas (business logic vs. technical implementation) |
| **Crypto Wallet Management** | Modern, interesting tech stack | Less regulated than traditional banking, fewer established best practices, less relevant to FinTech APIs | Outside scope of traditional banking compliance |
| **Virtual Card Lifecycle** ✅ | Perfect balance of complexity, compliance, security, and industry relevance | Requires deep understanding of payments industry | **CHOSEN** - Best demonstration of specification-driven design + FinTech expertise |

---

## Tech Stack Selection

### Components

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Runtime** | Node.js LTS | 20+ | JavaScript runtime environment |
| **Language** | TypeScript | 5+ | Type-safe JavaScript with strict mode |
| **Framework** | Express.js | 4.x | Lightweight web framework for REST APIs |
| **Database** | PostgreSQL | 15+ | ACID-compliant relational database |
| **ORM** | Prisma | 5+ | Type-safe database client with migrations |
| **Validation** | Zod | 3+ | TypeScript-first schema validation |
| **Financial Math** | decimal.js | latest | Arbitrary-precision decimal arithmetic |
| **Encryption** | Node.js crypto | built-in | AES-256-GCM encryption for PAN |
| **Logging** | Winston | 3+ | Structured logging with multiple transports |
| **Caching/Rate Limiting** | Redis (ioredis) | latest | Distributed rate limiting and caching |
| **Testing** | Jest + Supertest | 29+ | Unit and integration testing |

### Rationale: Why Node.js + TypeScript + PostgreSQL?

| Reason | Description |
|--------|-------------|
| **Industry Adoption** | **Stripe, Plaid, and Marqeta** (leading card issuing platforms) all use Node.js for their APIs. Demonstrates alignment with industry standards. Node.js is the de facto standard for modern FinTech APIs. |
| **Stack Diversity** | Previous homework assignments (1 & 2) used **Java/Spring Boot**. Choosing Node.js/TypeScript demonstrates **versatility** and ability to work across multiple tech stacks. Shows breadth of knowledge. |
| **Real-Time Capabilities** | Node.js event-driven architecture is ideal for **real-time card state changes**, webhook notifications, and high-concurrency API operations. Scales well for card issuing workloads. |
| **Rich Ecosystem** | Excellent libraries for FinTech requirements: decimal.js (financial precision), Zod (validation), Prisma (SQL injection prevention), crypto (PCI-DSS encryption), Winston (audit logging). |
| **Financial Precision** | **decimal.js** library prevents floating-point errors (0.1 + 0.2 = 0.30000000000004). Non-negotiable for monetary calculations. JavaScript ecosystem has best-in-class decimal arithmetic libraries. |

### Alternative: Java/Spring Boot

| Aspect | Java/Spring Boot | Node.js/TypeScript | Decision |
|--------|-----------------|-------------------|----------|
| **Type Safety** | Strong static typing | TypeScript provides static typing | Tie - both provide type safety |
| **Financial Libraries** | BigDecimal (built-in) | decimal.js (excellent) | Tie - both have robust decimal arithmetic |
| **Industry Use** | Traditional banks | Modern FinTech APIs (Stripe, Plaid) | **Node.js wins** - aligns with modern card issuing |
| **Concurrency** | Thread-based (complex) | Event-driven (simpler for I/O) | **Node.js wins** - better for API-heavy workloads |
| **Previous Use** | Used in homework 1 & 2 | New for this homework | **Node.js wins** - demonstrates versatility |

**Conclusion:** Node.js/TypeScript chosen for industry alignment and stack diversity.

---

## Industry Best Practices Demonstrated

This specification demonstrates **8 critical FinTech best practices** with specific references to where each practice appears in the specification documents.

---

### Practice 1: PCI-DSS Compliance

**What:** Payment Card Industry Data Security Standard - requirements for handling card data (PANs, CVVs, expiry dates).

**Why:** PCI-DSS compliance is **mandatory** for any system that stores, processes, or transmits cardholder data. Non-compliance can result in fines ($5,000-$100,000/month), loss of payment processing privileges, and reputational damage. Demonstrates understanding of regulated FinTech environments.

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **AES-256-GCM Encryption** | specification.md: Implementation Notes, Low-level Task 3 | Encrypt all card PAN data at rest using AES-256-GCM with unique IV per encryption. Never store plain text PAN. |
| **Tokenization** | specification.md: Implementation Notes, Low-level Task 3 | Generate UUID card_token for external references. Use token instead of PAN in all API operations. |
| **No CVV Storage** | specification.md: Implementation Notes, agents.md: Rule 4, .claude/rules/00-critical-constraints.md: NEVER #3 | NEVER store CVV/CVC/CID data, even encrypted. PCI-DSS SAQ A requirement violation. |
| **TLS 1.3** | specification.md: Implementation Notes | All data in transit must use TLS 1.3 or higher (disable TLS 1.0, 1.1, 1.2). |
| **Access Logging** | specification.md: Low-level Task 5, agents.md: Rule 5 | Log every access to card data (READ_CARD action) to audit_logs with user_id, IP, timestamp. |
| **Masked PAN Response** | specification.md: Low-level Task 5, .claude/rules/00-critical-constraints.md: ALWAYS #3 | API responses return only last 4 digits (****0366), never full PAN. |

**Industry Reference:** [PCI Security Standards Council - PCI DSS 4.0](https://www.pcisecuritystandards.org/)

---

### Practice 2: GDPR Compliance

**What:** EU General Data Protection Regulation - privacy requirements for collecting, processing, and storing personal data.

**Why:** GDPR applies to any organization handling EU citizens' data. Violations can result in fines up to €20M or 4% of global revenue. Demonstrates understanding of data privacy regulations and user rights.

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **Right to Deletion** | specification.md: Low-level Task 10, agents.md: Rule 6 | DELETE /api/v1/users/me endpoint anonymizes PII (name → "DELETED_USER") while preserving audit logs. |
| **Right to Export** | specification.md: Low-level Task 10 | GET /api/v1/users/me/data returns all user data (profile, masked cards, transactions) as JSON. |
| **7-Year Retention** | specification.md: Implementation Notes, agents.md: GDPR Checklist | Maintain data for 7 years per financial regulations, then automatic purge with scheduled job. |
| **Consent Management** | specification.md: Implementation Notes | Track user consent for data processing in database; require explicit opt-in. |
| **Purpose Limitation** | specification.md: Implementation Notes | Log purpose for each data access in audit trail. Only access card data for legitimate business purposes. |

**Industry Reference:** [GDPR Official Text - EU Regulation 2016/679](https://gdpr-info.eu/)

---

### Practice 3: Decimal Precision for Monetary Values

**What:** Use arbitrary-precision decimal arithmetic instead of floating-point for all monetary calculations.

**Why:** Floating-point arithmetic (IEEE 754) causes precision errors: `0.1 + 0.2 = 0.30000000000004`. In financial systems, precision errors accumulate and cause **incorrect balances, transaction amounts, and limit checks**. Using decimal.js is **non-negotiable** for monetary operations.

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **decimal.js Library** | specification.md: Implementation Notes, Tech Stack | Use decimal.js for ALL monetary calculations (amounts, limits, fees, balances). |
| **Max 2 Decimal Validation** | specification.md: Low-level Task 7, Low-level Task 12 | Zod schemas validate max 2 decimal places: `z.string().regex(/^\d+\.\d{0,2}$/)` |
| **Banker's Rounding** | specification.md: Implementation Notes, agents.md: Rule 1 | Configure `Decimal.set({ rounding: 6 })` for round half to even (banker's rounding). |
| **String/NUMERIC Storage** | specification.md: Implementation Notes, agents.md: Rule 1 | Store as PostgreSQL NUMERIC type or string (NEVER FLOAT, REAL, DOUBLE PRECISION). |

**Code Example:**

```typescript
// ❌ WRONG - Floating point errors
const dailyLimit = 0.1;
const fee = 0.2;
const total = dailyLimit + fee; // 0.30000000000004 (incorrect!)

// ✅ RIGHT - Exact precision with decimal.js
import Decimal from 'decimal.js';
Decimal.set({ rounding: 6 }); // Banker's rounding

const dailyLimit = new Decimal('0.1');
const fee = new Decimal('0.2');
const total = dailyLimit.plus(fee).toFixed(2); // "0.30" (correct!)

// Complex calculation chain
const spending = new Decimal('1234.56');
const cashback = spending.times('0.015').toFixed(2); // "18.52"
```

**Industry Reference:** [IEEE 754 Floating Point Problems - 0.30000000000000004.com](https://0.30000000000000004.com/)

---

### Practice 4: Comprehensive Audit Logging

**What:** Immutable, append-only audit trail that captures all state-changing operations with cryptographic hash chaining.

**Why:** Audit logs are **required** for SOX compliance, fraud investigation, dispute resolution, and regulatory reporting. Immutability prevents tampering. Hash chaining (SHA-256) provides tamper detection. 7-year retention satisfies financial regulations.

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **Append-Only Table** | specification.md: Low-level Task 9, agents.md: Rule 5, .claude/rules/00-critical-constraints.md: NEVER #8 | audit_logs table allows only INSERT operations (no UPDATE/DELETE). Immutability enforced. |
| **Hash Chaining** | specification.md: Low-level Task 9, agents.md: Rule 5 | Each record hash = SHA-256(previous_hash + current_data). Provides tamper detection. |
| **Before/After State** | specification.md: Low-level Task 6, Task 7, agents.md: Rule 5 | Log before_state and after_state (JSON) for all UPDATE operations. |
| **7-Year Retention** | specification.md: Implementation Notes, agents.md: Audit Checklist | Minimum 7 years per SOX compliance. Store in high-durability storage. |
| **Complete Metadata** | specification.md: Low-level Task 9 | Capture: user_id, action, resource_type, resource_id, ip_address, timestamp, before_state, after_state, hash. |

**Industry Reference:** [Sarbanes-Oxley Act (SOX) Audit Requirements](https://www.soxlaw.com/)

---

### Practice 5: Idempotency & Transaction Safety

**What:** Prevent duplicate operations from network retries using UUID-based idempotency keys and atomic database transactions.

**Why:** Network failures cause clients to retry requests. Without idempotency, retries create **duplicate cards, double charges, inconsistent state**. Idempotency keys enable safe retries. Atomic transactions ensure "all-or-nothing" execution (card creation + audit log).

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **UUID Keys** | specification.md: Low-level Task 4, Implementation Notes | Idempotency-Key header with UUID v4 for all POST/PATCH/DELETE operations. |
| **24h Deduplication** | specification.md: Low-level Task 4, Implementation Notes | Store idempotency results for 24 hours. Return cached response for duplicate keys. |
| **Atomic Transactions** | specification.md: Low-level Task 4, Task 6, agents.md: Rule 3 | Use Prisma.$transaction() to ensure card + audit log created together or not at all. |
| **Optimistic Locking** | specification.md: Low-level Task 6, Implementation Notes | Use version field or timestamp to prevent concurrent modification races. |

**Code Example:**

```typescript
// ✅ Idempotency pattern
async function createCard(userId: string, cardData: CreateCardDto, idempotencyKey: string) {
  // Step 1: Check if key exists (24h window)
  const existing = await prisma.idempotencyKey.findUnique({
    where: { key: idempotencyKey }
  });

  if (existing && existing.expires_at > new Date()) {
    return JSON.parse(existing.response); // Return exact same response
  }

  // Step 2: Process operation + store result atomically
  return await prisma.$transaction(async (tx) => {
    const card = await tx.card.create({ data: {...} });
    await tx.auditLog.create({ data: {...} });
    await tx.idempotencyKey.create({
      data: {
        key: idempotencyKey,
        response: JSON.stringify(result),
        expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000)
      }
    });
    return result;
  });
}
```

**Industry Reference:** [Stripe API Idempotency Documentation](https://stripe.com/docs/api/idempotent_requests)

---

### Practice 6: Rate Limiting & Fraud Prevention

**What:** Multi-tier rate limiting using Redis with per-user limits, global limits, and velocity checks.

**Why:** Prevents API abuse (DDoS attacks), brute force attacks, and fraud (mass card creation). Rate limiting is **critical** for FinTech APIs to prevent financial losses and ensure fair resource allocation. Velocity checks detect suspicious patterns (e.g., creating 100 cards in 5 minutes).

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **Per-User Limits** | specification.md: Low-level Task 11, Implementation Notes | 100 requests/minute per authenticated user (Redis key: `rate:user:{userId}:min`). |
| **Global Limits** | specification.md: Low-level Task 11, Implementation Notes | 1,000 requests/minute across all users (Redis key: `rate:global:min`). |
| **Velocity Checks** | specification.md: Low-level Task 11, Implementation Notes | Max 5 card creations per user per 24 hours (Redis key: `rate:user:{userId}:cards:24h`). |
| **Redis-Based** | specification.md: Low-level Task 11, agents.md: Rule 7 | Distributed rate limiting for horizontal scalability across multiple server instances. |
| **429 Response** | specification.md: Low-level Task 11, .claude/rules/05-error-handling.md: Pattern | Return 429 Too Many Requests with `Retry-After` header (seconds until reset). |

**Industry Reference:** [OWASP Rate Limiting Guide](https://owasp.org/www-community/controls/Blocking_Brute_Force_Attacks)

---

### Practice 7: Input Validation & SQL Injection Prevention

**What:** Strict input validation using Zod schemas and parameterized queries with Prisma ORM.

**Why:** Input validation prevents injection attacks (SQL, XSS, command injection) and data corruption. **SQL injection is #3 on OWASP Top 10 2021**. Parameterized queries (Prisma) prevent SQL injection. Zod provides type-safe validation with custom refinements (Luhn algorithm, decimal precision).

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **Zod Schemas** | specification.md: Low-level Task 12, agents.md: Section | Define schemas for all request bodies (CreateCardSchema, UpdateLimitsSchema, UpdateStateSchema). |
| **Luhn Algorithm** | specification.md: Low-level Task 12, .claude/rules/07-validation.md: Pattern | Validate card number checksum: `z.string().refine(luhnCheck, 'Invalid card number')`. |
| **Prisma ORM Only** | specification.md: Implementation Notes, agents.md: Anti-Patterns, .claude/rules/00-critical-constraints.md: NEVER #5 | Use Prisma for ALL database queries (prevents SQL injection). NEVER use raw SQL. |
| **Bounds Validation** | specification.md: Low-level Task 7, Task 12 | Spending limits: 0.01 to 1,000,000.00 with max 2 decimals. Reject out-of-range values. |

**Industry Reference:** [OWASP Top 10 2021 - A03:2021 Injection](https://owasp.org/Top10/A03_2021-Injection/)

---

### Practice 8: Secure Error Handling & No Data Leakage

**What:** Sanitized error responses that never expose stack traces, internal system details, or sensitive data in production.

**Why:** Stack traces reveal internal file paths, framework versions, and code structure - valuable information for attackers. **OWASP: "Information exposure through error messages"** is a common vulnerability. Sanitized errors protect system internals while providing useful feedback to clients.

**Specification Locations:**

| Practice Element | Spec Location | Description |
|-----------------|---------------|-------------|
| **No Stack Traces** | specification.md: Low-level Task 13, .claude/rules/00-critical-constraints.md: NEVER #6 | NEVER include err.stack in production responses (NODE_ENV check). |
| **Consistent Format** | specification.md: Low-level Task 13, Implementation Notes | All errors: `{ error: { code: 'CARD_NOT_FOUND', message: 'Card not found', details: {} } }` |
| **Custom Error Classes** | specification.md: Low-level Task 13, agents.md: Error Handling, .claude/rules/05-error-handling.md: Pattern | ValidationError, NotFoundError, UnauthorizedError with proper HTTP status codes. |
| **Internal Logging** | specification.md: Low-level Task 13 | Log full error with stack trace to Winston (level: error) for debugging. |

**Code Example:**

```typescript
// ✅ Atomic transaction pattern
await prisma.$transaction(async (tx) => {
  // All operations succeed together or fail together
  const card = await tx.card.update({ where: { id }, data: { state: 'FROZEN' } });
  await tx.auditLog.create({
    data: {
      action: 'FREEZE_CARD',
      before_state: { state: 'ACTIVE' },
      after_state: { state: 'FROZEN' }
    }
  });
  return card;
}, {
  isolationLevel: 'Serializable' // Highest isolation for financial data
});
```

**Industry Reference:** [OWASP Error Handling Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html)

---

## Specification Design Choices

### Breadth vs. Depth Balance

**Decision:** 15 low-level tasks covering 6 core operations

**Rationale:**
- **Breadth:** 6 operations (create, retrieve, freeze/unfreeze, limits, transactions, close) demonstrate full lifecycle management
- **Depth:** 15 detailed tasks with executable AI prompts show thorough decomposition of complex requirements
- **Balance:** Avoids both extremes: not too simple (3 tasks) nor overwhelming (50 tasks)
- **Result:** Sufficient complexity to demonstrate enterprise thinking while remaining implementable

### Specification Structure: Three-Tier Hierarchy

**Decision:** High-Level → Mid-Level → Low-Level structure

**Rationale:**
- **High-Level Objective (1 sentence):** "What" we're building - captures entire system in single sentence
- **Mid-Level Objectives (8 items):** "Why" and "what outcomes" - concrete, measurable, testable goals
- **Low-Level Tasks (15 items):** "How" to implement - specific AI prompts with files, functions, and requirements
- **Clarity:** Each level serves distinct purpose; no confusion about abstraction level
- **AI-Friendly:** Low-level tasks are directly executable by AI coding partners

### Task Format: Executable AI Prompts

**Decision:** Each task contains: Prompt | Files to CREATE/UPDATE | Functions to CREATE | Details

**Rationale:**
- **Prompt:** Exact instruction an AI agent would receive (copy-paste ready)
- **Files:** Explicit file paths (no ambiguity about where code goes)
- **Functions:** Specific function signatures to CREATE (clear interface contracts)
- **Details:** Technical requirements, validation rules, error handling, edge cases
- **Result:** AI agent can execute task without additional clarification

### Iteration Depth: 15-20 Implementation Notes

**Decision:** Comprehensive implementation notes covering tech stack, security, compliance, performance

**Rationale:**
- **Completeness:** AI agent has all context needed (no critical details missing)
- **Reference:** Serves as quick reference during implementation
- **Consistency:** Ensures all tasks follow same patterns (e.g., all use decimal.js, all create audit logs)
- **Quality:** 15-20 items is sweet spot (10 is too sparse, 30 is overwhelming)

---

## How to Use This Specification Package

### For AI Implementation

**Step 1:** Read `specification.md` to understand system objectives
- High-level objective: Single sentence describing entire system
- Mid-level objectives: 8 concrete goals (what to achieve)
- Implementation notes: Technical constraints and patterns (how to achieve)

**Step 2:** Review `agents.md` for domain rules and patterns
- 7 banking domain rules with code examples (decimal.js, idempotency, transactions, PCI-DSS, audit, GDPR, rate limiting)
- Tech stack details and rationale
- Security constraints and compliance checklists

**Step 3:** Check `.claude/rules/*.md` for coding standards
- Never/Always lists (8 critical constraints each)
- Naming conventions (files, classes, functions, database)
- Code patterns (financial calculations, security, testing, error handling, transactions, validation)
- Organized into 8 focused files by topic

**Step 4:** Execute low-level tasks sequentially from `specification.md`
- Each task is self-contained with prompt, files, functions, details
- Follow task order (dependencies are resolved sequentially)
- Create audit logs for all state changes
- Test after each task completion

### For Human Reviewers

**Step 1:** Read this `README.md` for context and rationale
- Understand why virtual cards were chosen
- Review tech stack justification
- See how 8 best practices map to specification

**Step 2:** Verify best practices mapping to spec locations
- Cross-reference tables in "Industry Best Practices" section
- Confirm each practice appears in specification.md, agents.md, and .claude/rules/*.md
- Check that code examples are consistent across documents

**Step 3:** Check cross-document consistency
- Tech stack versions match exactly (Node.js 20+, Express 4.x, PostgreSQL 15+, etc.)
- Terminology is consistent (card_token not cardToken, PAN not "card number", etc.)
- Best practices flow through all documents (e.g., decimal.js in spec → agents → rules → README)

**Step 4:** Review task completeness and quality
- Each low-level task has executable prompt (can copy-paste to AI agent)
- Files and functions are explicitly named (no ambiguity)
- Technical details cover edge cases, validation, error handling
- Tasks cover full lifecycle (create → manage → close)

### Verification Checklist

Use this checklist before considering the specification complete:

- [ ] All 8 best practices appear in `specification.md` (search for PCI-DSS, GDPR, decimal.js, audit, idempotency, rate limiting, validation, error handling)
- [ ] Tech stack versions consistent across all documents (Node.js 20+, Express 4.x, TypeScript 5+, PostgreSQL 15+, Prisma 5+, Zod 3+, decimal.js, Winston 3+, Jest 29+)
- [ ] Code examples work and demonstrate patterns clearly (decimal.js calculations, idempotency, transactions, audit logging)
- [ ] Spec locations in README tables match actual specification (grep for task numbers and section headers)
- [ ] Industry references are current and valid URLs (check links to PCI-DSS, GDPR, OWASP, Stripe)
- [ ] Low-level tasks have executable prompts (can hand to AI agent without modification)
- [ ] agents.md has 7 banking domain rules with working code examples
- [ ] .claude/rules/*.md has Never/Always lists (8 items each) and code patterns organized in 8 topic-focused files
- [ ] GDPR and PCI-DSS checklists are complete in agents.md
- [ ] decimal.js pattern shown in multiple documents (spec, agents, rules) with consistent syntax
- [ ] Audit logging pattern consistent across specification.md tasks and agents.md/rules.md code

---

## Lessons Learned / Design Rationale

### Why Virtual Cards Over Other Features?

**Decision:** Virtual card lifecycle instead of account transfers, loan origination, or crypto wallets

**Reasoning:**
1. **Industry Trend:** Card issuing APIs are dominant in modern FinTech (Stripe, Marqeta, Lithic, Privacy.com)
2. **Compliance Richness:** Demonstrates PCI-DSS, GDPR, SOX simultaneously (other features lack this breadth)
3. **Technical Depth:** Encryption, tokenization, audit logging, rate limiting, idempotency all required
4. **Clarity:** Card lifecycle has clear states and boundaries (easier to specify)
5. **Relevance:** Card management is more relevant to FinTech APIs than traditional banking operations

### Why Node.js Over Java for This Homework?

**Decision:** Node.js/TypeScript instead of Java/Spring Boot (used in homework 1 & 2)

**Reasoning:**
1. **Stack Diversity:** Demonstrates versatility across multiple ecosystems
2. **Industry Alignment:** Stripe, Plaid, Marqeta use Node.js (not Java)
3. **Concurrency Model:** Event-driven better for API-heavy card operations
4. **Ecosystem:** decimal.js, Zod, Prisma are excellent for FinTech requirements
5. **Learning:** Shows ability to apply FinTech best practices across different stacks

### Why .claude/rules/*.md Format?

**Decision:** .claude/rules/*.md (multiple files) instead of .github/copilot-instructions.md or single .cursor/rules.md

**Reasoning:**
1. **Student Context:** Already using Claude Code (evidenced by homework-2/.claude/)
2. **Organization:** Multi-file structure (8 files) allows focused, maintainable rules by topic
3. **Integration:** Native support in Claude Code for .claude/rules/ directory with multiple files
4. **Consistency:** Follows Claude Code best practices and .cursor/rules/*.md pattern
5. **Maintainability:** Each file covers one topic (constraints, naming, financial, security, testing, errors, transactions, validation)

### Depth of Specification: Enough for AI?

**Decision:** 15 low-level tasks with 20-50 lines each

**Reasoning:**
1. **Executable:** Each task has exact AI prompt (can copy-paste)
2. **Complete:** Files, functions, and details provided (no ambiguity)
3. **Tested Mentally:** Asked "Could an AI agent execute this without clarification?" for each task
4. **Balance:** Detailed enough for implementation, concise enough to review quickly
5. **Iterations:** Went through 3 drafts to refine task prompts for clarity

---

## Future Enhancements

These features are **out of scope** for this specification but could be added in future iterations:

1. **Multi-Currency Support with Real-Time FX Rates**
   - Support multiple currencies (USD, EUR, GBP, JPY)
   - Real-time exchange rate API integration (e.g., fixer.io, XE.com)
   - Currency conversion with decimal.js precision
   - Multi-currency spending limits

2. **Recurring Card Charges (Subscriptions)**
   - Schedule recurring transactions (daily, weekly, monthly)
   - Subscription management (create, pause, cancel)
   - Failed payment retry logic with exponential backoff
   - Webhook notifications for subscription events

3. **Dispute Management Workflow**
   - Users can file disputes on transactions
   - Multi-stage dispute workflow (filed → under review → resolved)
   - Evidence submission (receipts, screenshots)
   - Integration with card network dispute APIs (Visa, Mastercard)

4. **Real-Time Fraud Detection ML Model**
   - Train ML model on transaction patterns (amount, merchant, location, time)
   - Real-time scoring at transaction authorization
   - Automatic card freeze for high-risk transactions
   - Admin dashboard for fraud review queue

5. **Mobile SDKs (iOS/Android) for Card Management**
   - Native iOS SDK (Swift) and Android SDK (Kotlin)
   - Secure card data display (with biometric authentication)
   - Push notifications for transactions and state changes
   - Card freeze/unfreeze from mobile app

---

## References

### Industry Standards
- [PCI Security Standards Council - PCI DSS 4.0](https://www.pcisecuritystandards.org/)
- [GDPR Official Text - EU Regulation 2016/679](https://gdpr-info.eu/)
- [Sarbanes-Oxley Act (SOX) Compliance](https://www.soxlaw.com/)
- [OWASP Top 10 2021](https://owasp.org/Top10/)

### FinTech API References
- [Stripe Issuing API Documentation](https://stripe.com/docs/issuing)
- [Stripe API Idempotency](https://stripe.com/docs/api/idempotent_requests)
- [Marqeta Card Issuing Platform](https://www.marqeta.com/)

### Technical Resources
- [Node.js Crypto Module](https://nodejs.org/api/crypto.html)
- [Prisma ORM Documentation](https://www.prisma.io/docs)
- [decimal.js Library](https://github.com/MikeMcl/decimal.js/)
- [Zod Schema Validation](https://zod.dev/)
- [IEEE 754 Floating Point Issues](https://0.30000000000000004.com/)
- [PostgreSQL ACID Guarantees](https://www.postgresql.org/docs/current/tutorial-transactions.html)

---

## Acknowledgments

**Course:** AI Coding Partner

This specification package demonstrates the application of specification-driven design principles to a real-world FinTech use case. The focus on PCI-DSS, GDPR, and financial precision reflects the critical importance of security, compliance, and correctness in regulated industries.

Special thanks to the open-source community for excellent libraries (Prisma, Zod, decimal.js, Jest) that make building secure, compliant financial applications feasible.

---

**End of README**

# Banking Transaction Processing Pipeline — Project Context

## Project Overview

This project implements a 3-agent Node.js/TypeScript pipeline that processes banking transactions through sequential stages: validation, fraud detection, and reporting. Agents communicate via file-based JSON message passing through a shared/ directory structure, making each stage independently testable and observable.

**Entry point**: `sample-transactions.json` (8 raw transactions)
**Exit point**: `shared/results/pipeline_summary.json` + 8 individual result files

---

## Technology Stack

| Concern | Library / Tool | Notes |
|---|---|---|
| Runtime | Node.js 20+ | LTS recommended |
| Language | TypeScript 5.x | Strict mode enabled |
| Execution | ts-node | Direct .ts execution without pre-compile |
| Decimal arithmetic | decimal.js | All monetary values; never use native floats |
| Testing | Jest + ts-jest | Unit and integration tests |
| Linting | ESLint + @typescript-eslint | Enforce consistent style |
| File I/O | Node.js `fs/promises` | Async throughout |

---

## Agent Communication Protocol

Every message passed between agents is a JSON object conforming to this envelope:

```json
{
  "message_id": "uuid-v4",
  "timestamp": "2026-03-16T09:00:00.000Z",
  "source_agent": "transaction_validator",
  "target_agent": "fraud_detector",
  "data": {
    "transaction_id": "TXN001",
    "amount": "1500.00",
    "currency": "USD",
    "source_account": "ACC-1001",
    "destination_account": "ACC-2001",
    "transaction_type": "transfer",
    "description": "Monthly rent payment",
    "metadata": { "channel": "online", "country": "US" },
    "status": "validated",
    "fraud_risk_score": 0,
    "fraud_risk_level": "LOW"
  }
}
```

**Key fields added at each stage:**

| Stage | Fields Added |
|---|---|
| Transaction Validator | `status` ("validated" \| "rejected"), `rejection_reason` (if rejected) |
| Fraud Detector | `fraud_risk_score` (0–10), `fraud_risk_level` ("LOW" \| "MEDIUM" \| "HIGH" \| "N/A") |
| Reporting Agent | `processed_at` (ISO 8601), writes final result file |

---

## Directory Structure

```
homework-6/
├── sample-transactions.json       # Source: 8 raw transactions
├── agents/
│   ├── transaction_validator.ts   # Agent 1
│   ├── fraud_detector.ts          # Agent 2
│   └── reporting_agent.ts         # Agent 3
├── shared/
│   ├── input/                     # Raw transactions copied here before pipeline runs
│   ├── output/                    # Validator writes here → Fraud detector reads here
│   ├── processing/                # Fraud detector writes here → Reporting agent reads here
│   ├── results/                   # Final per-transaction results + pipeline_summary.json
│   └── logs/                      # Optional: structured audit log files
├── tests/
│   ├── transaction_validator.test.ts
│   ├── fraud_detector.test.ts
│   └── reporting_agent.test.ts
├── pipeline.ts                    # Orchestrator: runs all 3 agents in sequence
├── tsconfig.json
└── package.json
```

---

## Core Principles

### 1. Decimal Precision
All monetary amounts are stored as strings in JSON and converted to `decimal.js` `Decimal` objects for any arithmetic or comparison. Example:
```typescript
import { Decimal } from 'decimal.js';
const amount = new Decimal(transaction.amount);
if (amount.isNegative()) { /* reject */ }
if (amount.greaterThan(new Decimal('10000'))) { /* flag */ }
```

### 2. Security — PII Masking
Account numbers must never appear unmasked in logs. Always apply:
```typescript
const maskAccount = (account: string): string =>
  '****' + account.slice(-4);
```

### 3. Audit Logging
Every agent logs each decision in structured format:
```
[2026-03-16T09:00:00.000Z] [transaction_validator] TXN001 → VALIDATED
[2026-03-16T10:05:00.000Z] [transaction_validator] TXN006 → REJECTED (INVALID_CURRENCY)
```

### 4. Error Handling
- Wrap each transaction in try/catch; log the error and continue with remaining transactions
- Never let one bad transaction crash the entire pipeline
- On unrecoverable errors (e.g., shared/ directory unwritable), throw and exit with code 1

---

## Expected Test Coverage

Each agent must have unit tests covering:

| Test Case | Agent | Expected Outcome |
|---|---|---|
| Valid transaction | Validator | status: "validated" |
| Negative amount (TXN007) | Validator | rejected, NEGATIVE_AMOUNT |
| Invalid currency (TXN006) | Validator | rejected, INVALID_CURRENCY |
| Missing required field | Validator | rejected, MISSING_FIELD |
| Amount > $10,000 (TXN002 $25k) | Fraud Detector | score ≥ 3, MEDIUM |
| Amount > $50,000 (TXN005 $75k) | Fraud Detector | score ≥ 7, HIGH |
| Unusual hour 02–04 UTC (TXN004) | Fraud Detector | score includes +2 |
| Cross-border transaction (TXN004 DE) | Fraud Detector | score includes +1 |
| Rejected transaction pass-through | Fraud Detector | fraud_risk_level: "N/A" |
| All 8 transactions produce result files | Reporting Agent | 8 files in shared/results/ |
| Correct rejection_breakdown in summary | Reporting Agent | { INVALID_CURRENCY: 1, NEGATIVE_AMOUNT: 1 } |
| Correct risk_distribution in summary | Reporting Agent | counts match scored transactions |

Minimum coverage target: **90%** (lines + branches).

---

## Running the Pipeline

```bash
# Install dependencies
npm install

# Run full pipeline
npx ts-node pipeline.ts

# Run tests
npm test

# Run a single agent (for debugging)
npx ts-node agents/transaction_validator.ts
```

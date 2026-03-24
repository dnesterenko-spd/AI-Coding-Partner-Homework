# Specification: Banking Transaction Processing Pipeline

## 1. High-Level Objective

Build a 3-agent Node.js/TypeScript pipeline that validates, scores for fraud risk, and generates reports for banking transactions using file-based JSON message passing.

## 2. Mid-Level Objectives

- Transactions with invalid currency codes (like "XYZ") are rejected with reason "INVALID_CURRENCY"
- Transactions with negative amounts are rejected with reason "NEGATIVE_AMOUNT"
- Transactions above $10,000 are flagged with fraud_risk_level: "MEDIUM" or higher
- Transactions above $50,000 are flagged with fraud_risk_level: "HIGH"
- All 8 transactions from sample-transactions.json produce result files in shared/results/
- A final pipeline summary report is written to shared/results/pipeline_summary.json

## 3. Implementation Notes

- Monetary calculations: use `decimal.js` (`import { Decimal } from 'decimal.js'`) for all amount comparisons and arithmetic — never use native floating-point
- Currency validation: ISO 4217 whitelist — USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY
- Logging: audit trail with ISO 8601 timestamp, agent_name, transaction_id, outcome written to console and to shared/logs/
- PII: mask account numbers in logs (show only last 4 chars, e.g., "****1001")
- Error handling: all exceptions caught, logged with stack trace, pipeline continues with remaining transactions
- Concurrency: Agent 1 → Agent 2 → Agent 3 sequential per transaction; multiple transactions may be dispatched in parallel with Promise.all

## 4. Context

- **Beginning state**: `sample-transactions.json` exists with 8 raw transaction records. No agents exist. No shared/ directories exist.
- **Ending state**: All 8 transactions processed. Individual result files in `shared/results/<transaction_id>.json` with accept/reject status and fraud scores. A `shared/results/pipeline_summary.json` report summarising totals, rejected reasons, and risk distribution. Test coverage ≥ 90%.

## 5. Low-Level Tasks

### Task: Transaction Validator

**Prompt**: "Create a transaction validation agent following these specifications:

Context: Node.js/TypeScript pipeline processing banking transactions from sample-transactions.json. Message format uses JSON with message_id, timestamp, source_agent, target_agent, and data fields. The shared/input/ directory contains one JSON file per transaction. Use TypeScript with ts-node for execution.

Task: Build an agent that reads transactions from shared/input/, validates each one, writes the validated or rejected message to shared/output/, and logs the outcome.

Rules:
- Use `decimal.js` (`import { Decimal } from 'decimal.js'`) for ALL monetary comparisons — never use parseFloat or native arithmetic on amounts
- Required fields: transaction_id, amount, currency, source_account, destination_account
- Reject negative amounts with reason NEGATIVE_AMOUNT
- Reject invalid currencies (not in whitelist: USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY) with reason INVALID_CURRENCY
- Mask account numbers in all log output (show only last 4 chars, prefix with ****)
- Log every validation decision with ISO 8601 timestamp and transaction_id

Examples:
- Input `{ amount: '-100.00', currency: 'GBP' }` → rejected, reason: 'NEGATIVE_AMOUNT'
- Input `{ amount: '200.00', currency: 'XYZ' }` → rejected, reason: 'INVALID_CURRENCY'
- Input `{ amount: '1500.00', currency: 'USD' }` → validated, status: 'validated'

Output: TypeScript file `agents/transaction_validator.ts` exporting a `processMessage(message: TransactionMessage): ValidatedMessage` function. Also export a `runValidator(): Promise<void>` entry point that reads from shared/input/ and writes to shared/output/."

**File to CREATE**: `agents/transaction_validator.ts`
**Functions to CREATE**:
- `processMessage(message: TransactionMessage): ValidatedMessage`
- `runValidator(): Promise<void>`

**Details**:
- Read all .json files from shared/input/
- For each: validate required fields, amount sign, currency whitelist
- Write result to shared/output/<transaction_id>.json with status: "validated" | "rejected" and optional rejection_reason
- Create shared/output/ directory if it does not exist

---

### Task: Fraud Detector

**Prompt**: "Create a fraud detection agent following these specifications:

Context: Node.js/TypeScript pipeline receiving validated transactions from shared/output/. Uses a scoring system to assess fraud risk. Depends on decimal.js and date-fns (or native Date) for timestamp parsing.

Task: Read validated transactions from shared/output/, score each for fraud risk, write scored messages to shared/processing/, and log the assessment.

Rules:
- Use `decimal.js` for all amount comparisons
- Scoring (cumulative, 0–10 scale):
  - amount > $10,000: +3 points
  - amount > $50,000: +4 additional points (total +7 for amounts > $50,000)
  - transaction timestamp hour between 02:00–04:59 UTC: +2 points
  - metadata.country differs from destination account's inferred country (cross-border heuristic: non-US country code): +1 point
- Risk levels: LOW (0–2), MEDIUM (3–6), HIGH (7–10)
- Skip rejected transactions (pass them through unchanged)
- Log each scoring decision with transaction_id, score, and risk level

Examples:
- TXN002 $25,000 USD → score 3 → MEDIUM
- TXN004 $500 EUR at 02:47 UTC from DE → score 3 (cross-border +1, unusual hour +2) → MEDIUM
- TXN005 $75,000 USD → score 7 → HIGH

Output: TypeScript file `agents/fraud_detector.ts` exporting `processMessage(message: ValidatedMessage): ScoredMessage` and `runFraudDetector(): Promise<void>`."

**File to CREATE**: `agents/fraud_detector.ts`
**Functions to CREATE**:
- `processMessage(message: ValidatedMessage): ScoredMessage`
- `runFraudDetector(): Promise<void>`

**Details**:
- Read all .json files from shared/output/
- For each validated transaction: compute fraud_risk_score and fraud_risk_level
- Rejected transactions forwarded with fraud_risk_score: 0, fraud_risk_level: "N/A"
- Write to shared/processing/<transaction_id>.json
- Create shared/processing/ directory if it does not exist

---

### Task: Reporting Agent

**Prompt**: "Create a reporting agent following these specifications:

Context: Node.js/TypeScript pipeline. All 8 transactions have been processed through validation and fraud detection and their scored messages sit in shared/processing/. This is the final stage of the pipeline.

Task: Read all scored transaction messages from shared/processing/, write an individual result file for each transaction to shared/results/, then generate a consolidated pipeline_summary.json report.

Rules:
- Write one result file per transaction to shared/results/<transaction_id>.json containing: transaction_id, status (accepted | rejected), rejection_reason (if rejected), fraud_risk_score, fraud_risk_level, processed_at (ISO 8601)
- pipeline_summary.json must include:
  - total_transactions: 8
  - accepted_count, rejected_count
  - rejection_breakdown: object mapping rejection_reason → count (e.g., { INVALID_CURRENCY: 1, NEGATIVE_AMOUNT: 1 })
  - risk_distribution: object mapping risk level → count (e.g., { LOW: 3, MEDIUM: 2, HIGH: 1, 'N/A': 2 })
  - generated_at: ISO 8601 timestamp
- Mask account numbers in all log output
- Log a summary line to console upon completion

Examples:
- TXN006 (rejected, INVALID_CURRENCY) → shared/results/TXN006.json with status: 'rejected', rejection_reason: 'INVALID_CURRENCY'
- TXN005 (accepted, HIGH) → shared/results/TXN005.json with status: 'accepted', fraud_risk_level: 'HIGH'
- pipeline_summary.json rejection_breakdown: { INVALID_CURRENCY: 1, NEGATIVE_AMOUNT: 1 }

Output: TypeScript file `agents/reporting_agent.ts` exporting `processMessage(message: ScoredMessage): ResultRecord` and `runReportingAgent(): Promise<void>` that writes all results and the summary."

**File to CREATE**: `agents/reporting_agent.ts`
**Functions to CREATE**:
- `processMessage(message: ScoredMessage): ResultRecord`
- `runReportingAgent(): Promise<void>`

**Details**:
- Read all .json files from shared/processing/
- Write individual result to shared/results/<transaction_id>.json
- Aggregate totals and write shared/results/pipeline_summary.json
- Create shared/results/ directory if it does not exist
- Log final summary counts to console

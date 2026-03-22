Generate a complete technical specification for an AI-powered multi-agent banking transaction pipeline.

## Usage

### With Arguments (Direct Mode)
Pass requirements as arguments to skip interactive questions:
```
/write-spec language:python agent:compliance-checker
/write-spec language:nodejs agent:settlement-processor
/write-spec language:java agent:reporting-agent
```

**Supported options:**
- `language`: python, nodejs, java, go
- `agent`: compliance-checker, settlement-processor, reporting-agent

### Without Arguments (Interactive Mode)
Run without arguments to be prompted for choices:
```
/write-spec
```

## Process

1. **Gather Requirements**
   - Parse provided arguments OR use AskUserQuestion to interactively gather:
     - Programming language (Python, Node.js/TypeScript, Java, Go)
     - Third agent choice (Compliance Checker, Settlement Processor, or Reporting Agent)
     - Any additional requirements or constraints

2. **Analyze Sample Data**
   - Read `sample-transactions.json`
   - Identify patterns and edge cases:
     - TXN002: $25,000 high-value transaction
     - TXN004: 2:47am unusual hour EUR transaction
     - TXN005: $75,000 very high-value transaction
     - TXN006: Invalid "XYZ" currency (should be rejected)
     - TXN007: Negative amount -100.00 (should be rejected)
   - Use these patterns to create testable objectives

3. **Generate specification.md**
   Following the exact template structure:

```markdown
# Specification: Banking Transaction Processing Pipeline

## 1. High-Level Objective
[One sentence based on chosen language, e.g., "Build a 3-agent Python pipeline that validates, scores for fraud risk, and processes banking transactions using file-based JSON message passing."]

## 2. Mid-Level Objectives
- Transactions with invalid currency codes (like "XYZ") are rejected with reason "INVALID_CURRENCY"
- Transactions with negative amounts are rejected with reason "NEGATIVE_AMOUNT"
- Transactions above $10,000 are flagged with fraud_risk_level: "MEDIUM" or higher
- Transactions above $50,000 are flagged with fraud_risk_level: "HIGH"
- All 8 transactions from sample-transactions.json produce result files in shared/results/

## 3. Implementation Notes
- Monetary calculations: [language-specific: decimal.Decimal for Python, decimal.js for Node, BigDecimal for Java, shopspring/decimal for Go]
- Currency validation: ISO 4217 whitelist (USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY)
- Logging: audit trail with ISO 8601 timestamp, agent_name, transaction_id, outcome
- PII: mask account numbers in logs (show only last 4 chars, e.g., "****2001")
- Error handling: all exceptions logged, graceful degradation
- Concurrency: agents can process messages in parallel where possible

## 4. Context
- **Beginning state**: `sample-transactions.json` exists with 8 raw transaction records. No agents exist. No shared/ directories exist.
- **Ending state**: All 8 transactions processed. Results in `shared/results/` with accept/reject status. Pipeline summary report generated. Test coverage ≥ 90%.

## 5. Low-Level Tasks

### Task: Transaction Validator
**Prompt**: "Create a transaction validation agent following these specifications:
Context: [Language] pipeline processing banking transactions from sample-transactions.json. Message format uses JSON with message_id, timestamp, source_agent, target_agent, and data fields.
Task: Build an agent that validates incoming transactions for required fields, positive amounts, and valid ISO 4217 currency codes.
Rules:
- Use [decimal type for language] for all monetary calculations
- Required fields: transaction_id, amount, currency, source_account, destination_account
- Reject negative amounts with reason NEGATIVE_AMOUNT
- Reject invalid currencies (not in whitelist) with reason INVALID_CURRENCY
- Log all validations with timestamp and transaction_id
Examples: Input transaction with amount: '-100.00' should be rejected. Currency 'XYZ' should be rejected.
Output: Python file agents/transaction_validator.py with process_message(message: dict) -> dict function"
**File to CREATE**: agents/transaction_validator.[ext]
**Function to CREATE**: process_message(message: dict) -> dict
**Details**:
- Validate required fields presence and format
- Check amount is positive using decimal type
- Validate currency against ISO 4217 whitelist
- Return message with status: "validated" or "rejected" plus reason field
- Write to shared/output/ for next agent

### Task: Fraud Detector
**Prompt**: "Create a fraud detection agent following these specifications:
Context: [Language] pipeline receiving validated transactions. Uses scoring system for risk assessment.
Task: Score transactions for fraud risk based on amount, time, and cross-border indicators.
Rules:
- Score 0-10 scale: amount > $10,000 (+3), > $50,000 (+4), unusual hour 2-5am (+2), cross-border (+1)
- Risk levels: LOW (0-2), MEDIUM (3-6), HIGH (7-10)
- Use [decimal type] for amount comparisons
- Parse ISO 8601 timestamps for hour checking
Examples: $75,000 transaction = HIGH risk. 2:47am EUR transaction = elevated risk.
Output: [Language] file with process_message function returning fraud_risk_score and fraud_risk_level"
**File to CREATE**: agents/fraud_detector.[ext]
**Function to CREATE**: process_message(message: dict) -> dict
**Details**:
- Calculate fraud score based on rules
- Assign risk level based on score ranges
- Add fraud_risk_score and fraud_risk_level to message
- Forward to next agent via shared/output/

### Task: [Chosen Third Agent]
[Details vary based on selection: Compliance Checker, Settlement Processor, or Reporting Agent]
```

4. **Generate agents.md**
   Create comprehensive project context including:
   - Project overview and purpose
   - Technology stack table for chosen language
   - Agent communication protocol with JSON message format
   - Directory structure (shared/input/, processing/, output/, results/)
   - Core principles (decimal precision, security, audit logging)
   - Testing requirements and coverage goals

5. **Validate Output**
   Ensure:
   - All 5 sections present in specification.md
   - Testable objectives match sample data patterns
   - Each agent has complete prompt with Context-Task-Rules-Examples-Output
   - Language-specific implementation details are correct
   - File extensions and import statements match chosen language

## Prompt Engineering Format

Every agent prompt MUST follow this structure:
```
Context: [What exists - technology stack, files, constraints]
Task: [Exactly what to build]
Rules: [Non-negotiable requirements - decimal handling, logging, validation]
Examples: [Sample inputs/outputs from sample-transactions.json]
Output: [Expected file format and function signature]
```

## Notes
- Standard fraud thresholds: $10,000 for high-value, $50,000 for very-high, 2-5am for unusual hours
- The specification addresses all edge cases in sample-transactions.json
- Each agent processes messages from shared/ directories in sequence
- Final results must include all 8 transactions with clear accept/reject status
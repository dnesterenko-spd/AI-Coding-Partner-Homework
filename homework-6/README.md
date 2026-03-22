# AI-Powered Multi-Agent Banking Transaction Pipeline

**Created by Dmytro Nesterenko**

## Overview

This project implements a sophisticated multi-agent transaction processing pipeline for banking operations. The system employs three specialized AI agents working in sequence to validate, analyze, and report on financial transactions. Built with TypeScript and Node.js, the pipeline demonstrates enterprise-grade patterns including file-based message passing, decimal precision for monetary calculations, comprehensive fraud detection, and detailed audit logging.

The pipeline processes banking transactions through a series of intelligent agents, each responsible for a specific aspect of transaction verification and risk assessment. Transactions flow through validation checks, fraud risk scoring, and final reporting, with each stage maintaining strict data integrity and providing detailed logging for audit purposes. The system is designed to handle various transaction types, currencies, and edge cases while maintaining PII security through account number masking.

## Architecture

```
┌────────────────────┐
│ Transactions.json  │
└────────┬───────────┘
         ↓
    shared/input/
         ↓
┌────────┴───────────┐
│ Transaction        │
│ Validator          │
└────────┬───────────┘
         ↓
   shared/output/
         ↓
┌────────┴───────────┐
│ Fraud Detector     │
│ Agent              │
└────────┬───────────┘
         ↓
  shared/processing/
         ↓
┌────────┴───────────┐
│ Reporting Agent    │
└────────┬───────────┘
         ↓
   shared/results/
         ↓
  [Results JSON &
   Pipeline Summary]
```

## Agent Responsibilities

### Transaction Validator Agent (`agents/transaction_validator.ts`)
- Validates all required transaction fields (transaction_id, amount, currency, source_account, destination_account)
- Ensures amounts are positive and non-zero
- Verifies currency codes against ISO 4217 standards (USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY)
- Masks sensitive account numbers in logs (showing only last 4 digits)
- Outputs validated transactions to `shared/output/` or rejects with specific reason codes

### Fraud Detector Agent (`agents/fraud_detector.ts`)
- Scores each validated transaction for fraud risk on a 0-10 scale
- Analyzes transaction patterns including:
  - High-value transactions (>$10,000 adds 3 points, >$50,000 adds 7 points)
  - Unusual timing (2:00-4:59 AM UTC adds 2 points)
  - Cross-border transactions (non-US countries add 1 point)
- Categorizes risk levels: LOW (0-2), MEDIUM (3-6), HIGH (7-10)
- Preserves rejection status for invalid transactions with N/A risk level
- Outputs scored transactions to `shared/processing/`

### Reporting Agent (`agents/reporting_agent.ts`)
- Generates individual result files for each transaction in `shared/results/`
- Creates comprehensive pipeline summary with statistics:
  - Total transactions processed
  - Accept/reject counts with breakdown by rejection reason
  - Risk distribution across LOW, MEDIUM, HIGH categories
- Adds processing timestamps to all results
- Produces both machine-readable JSON and human-readable summaries

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Node.js** | 20+ | Runtime environment |
| **TypeScript** | 5.x | Type-safe development |
| **ts-node** | 10.9.x | TypeScript execution |
| **decimal.js** | 10.4.x | Precise decimal arithmetic for monetary values |
| **uuid** | 9.0.x | Unique message ID generation |
| **Jest** | 29.5.x | Testing framework |
| **ts-jest** | 29.1.x | TypeScript test support |
| **FastMCP** | 3.34.x | MCP server for external queries |
| **ESLint** | 8.x | Code quality and standards |
| **Zod** | 4.3.x | Runtime schema validation |

## Features

- **File-Based Message Passing**: Agents communicate through structured JSON files in shared directories
- **Decimal Precision**: All monetary calculations use decimal.js to prevent floating-point errors
- **ISO Standards Compliance**: Currency validation against ISO 4217 codes
- **Comprehensive Testing**: 99%+ code coverage with unit and integration tests
- **PII Protection**: Account numbers automatically masked in all logs
- **Audit Trail**: ISO 8601 timestamps on every operation for compliance
- **MCP Integration**: External tools for querying transaction status and pipeline results
- **Custom Skills**: Claude Code slash commands for pipeline automation
- **Git Hooks**: Automated coverage verification blocking pushes below 80%

## Sample Transactions

The system includes 8 test transactions covering various scenarios:
- Normal transactions (accepted with LOW risk)
- High-value transactions (>$10,000 triggers MEDIUM risk, >$50,000 triggers HIGH risk)
- Invalid currency codes (rejected with INVALID_CURRENCY)
- Negative amounts (rejected with NEGATIVE_AMOUNT)
- Cross-border and unusual hour transactions (increased risk scores)

## Quick Start

```bash
# Install dependencies
npm install

# Run the complete pipeline
npm run pipeline

# Run tests with coverage
npm run test:coverage

# Start MCP server
npm run mcp
```

For detailed setup and usage instructions, see [HOWTORUN.md](./HOWTORUN.md).

## Testing

The project includes comprehensive test coverage (99%+ statements, 92%+ functions):
- Unit tests for each agent's core logic
- Integration tests for file I/O operations
- End-to-end pipeline tests
- Automated coverage gate preventing pushes below 80%

Run tests:
```bash
npm test                    # Run all tests
npm run test:coverage       # Run with coverage report
npm run test:coverage:check # Verify 80% minimum coverage
```

## MCP Server Integration

The project includes a FastMCP server providing:
- **`get_transaction_status`**: Query individual transaction results
- **`list_pipeline_results`**: Get comprehensive pipeline summary
- **`pipeline://summary`**: Access latest run summary as a resource

## Skills and Automation

Custom Claude Code skills available:
- `/run-pipeline`: Execute the full pipeline end-to-end
- `/validate-transactions`: Dry-run validation without processing
- `/write-spec`: Generate technical specifications

## Directory Structure

```
homework-6/
├── agents/                 # Agent implementations
├── shared/                 # Message passing directories
│   ├── input/             # Initial transactions
│   ├── output/            # Validated transactions
│   ├── processing/        # Scored transactions
│   └── results/           # Final results
├── tests/                 # Comprehensive test suite
├── mcp/                   # MCP server implementation
├── .claude/commands/      # Custom skill definitions
└── docs/                  # Documentation and screenshots
```

## License

MIT License - See LICENSE file for details

## Author

**Dmytro Nesterenko** - Banking Transaction Pipeline Implementation
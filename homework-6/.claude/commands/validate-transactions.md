Validate all transactions in sample-transactions.json without processing them.

## Steps

1. **Run the validator in dry-run mode**
   Create a TypeScript script that:
   - Imports the `processMessage` function from `agents/transaction_validator.ts`
   - Loads all transactions from `sample-transactions.json`
   - Calls `processMessage()` on each transaction in memory
   - Does NOT write any files to shared/ directories

   Execute the script using: `npx ts-node -e "[inline script]"`

2. **Report: total count, valid count, invalid count, reasons for rejection**
   Display a summary table:

   | Metric | Count |
   |--------|-------|
   | Total Transactions | 8 |
   | Valid | 6 |
   | Invalid | 2 |

   Show detailed rejection information:

   | Transaction ID | Amount | Currency | Status | Rejection Reason |
   |---------------|--------|----------|--------|------------------|
   | TXN001 | 1500.00 | USD | Valid | - |
   | TXN002 | 25000.00 | USD | Valid | - |
   | TXN003 | 9999.99 | USD | Valid | - |
   | TXN004 | 500.00 | EUR | Valid | - |
   | TXN005 | 75000.00 | USD | Valid | - |
   | TXN006 | 200.00 | XYZ | Rejected | INVALID_CURRENCY |
   | TXN007 | -100.00 | GBP | Rejected | NEGATIVE_AMOUNT |
   | TXN008 | 3200.00 | USD | Valid | - |

3. **Show a table of results**
   Format the output clearly showing:
   - Which transactions passed validation
   - Which transactions failed and why
   - Summary statistics

## Implementation

The validation script should:
```typescript
import { processMessage } from './agents/transaction_validator';
import transactions from './sample-transactions.json';

const results = transactions.map(txn => ({
  ...txn,
  result: processMessage(txn)
}));

// Display results without any file I/O
```

## Expected Results

- TXN006 should be rejected for INVALID_CURRENCY (XYZ not in whitelist)
- TXN007 should be rejected for NEGATIVE_AMOUNT (-100.00)
- All other transactions should pass validation

## Notes

This is a dry-run validation that:
- Does NOT create or modify any files
- Does NOT use the shared/ directory structure
- Runs the validation logic directly in memory
- Provides immediate feedback on transaction validity
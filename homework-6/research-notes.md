# Research Notes: Context7 MCP Queries

## Query 1: Decimal Arithmetic for Monetary Calculations

**Search**: decimal.js comparison operations for monetary amounts in Node.js/TypeScript

**Context7 Library ID**: `/mikemcl/decimal.js`

**Key Insights Applied**:
- Used `Decimal.greaterThan()`, `Decimal.lessThan()`, and `Decimal.greaterThanOrEqualTo()` methods for amount comparisons in fraud detection scoring
- Never use native JavaScript floating-point operations (which have precision errors); always wrap monetary amounts with `new Decimal(value)`
- For fraud detection thresholds: `amount.greaterThan(new Decimal('10000'))` for $10k+ detection, `amount.greaterThan(new Decimal('50000'))` for $50k+ detection
- Used `.isNegative()` method to validate transaction amounts in the validator agent

**Code Pattern Applied**:
```typescript
import { Decimal } from 'decimal.js';
const amount = new Decimal(transaction.amount);
if (amount.isNegative()) {
  // Reject transaction
}
if (amount.greaterThan(new Decimal('10000'))) {
  // Increment fraud score
}
```

---

## Query 2: Node.js fs/promises for Async File I/O

**Search**: Node.js fs.promises readFile writeFile readdir async file operations

**Context7 Library ID**: `/websites/nodejs_api`

**Key Insights Applied**:
- Used `fs.promises.readFile(path, { encoding: 'utf8' })` to read JSON transaction files from shared/ directories
- Used `fs.promises.writeFile(path, JSON.stringify(message, null, 2))` for atomic writes of agent messages
- Used `fs.promises.readdir(path)` to list all transaction files in a directory without blocking the event loop
- All file operations wrapped in async/await with try-catch for robust error handling

**Code Pattern Applied**:
```typescript
import { readFile, writeFile, readdir } from 'node:fs/promises';

// Reading JSON files
const fileContent = await readFile(filePath, { encoding: 'utf8' });
const message = JSON.parse(fileContent);

// Writing JSON results
await writeFile(outputPath, JSON.stringify(result, null, 2));

// Listing files in a directory
const files = await readdir(inputDir);
```

---

## Implementation Summary

Both queries directly informed the agent architecture:
- **Decimal precision** ensures accurate fraud scoring and validation logic
- **Async file I/O** enables the file-based message passing protocol between agents without blocking
- All three agents use these patterns consistently for reliable transaction processing

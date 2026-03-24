import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { Decimal } from 'decimal.js';
import { v4 as uuidv4 } from 'uuid';
import {
  TransactionData,
  ValidationResult,
  Message,
  VALID_CURRENCIES,
  maskAccount,
  getISOTimestamp,
} from '../types';

const REQUIRED_FIELDS = ['transaction_id', 'amount', 'currency', 'source_account', 'destination_account'];

export function processMessage(data: TransactionData): ValidationResult {
  // Check required fields
  for (const field of REQUIRED_FIELDS) {
    if (!data[field as keyof TransactionData]) {
      return {
        ...data,
        status: 'rejected',
        rejection_reason: 'MISSING_FIELD',
      };
    }
  }

  // Check amount is positive
  const amount = new Decimal(data.amount);
  if (amount.isNegative()) {
    return {
      ...data,
      status: 'rejected',
      rejection_reason: 'NEGATIVE_AMOUNT',
    };
  }

  // Check currency is valid
  if (!VALID_CURRENCIES.includes(data.currency.toUpperCase())) {
    return {
      ...data,
      status: 'rejected',
      rejection_reason: 'INVALID_CURRENCY',
    };
  }

  return {
    ...data,
    status: 'validated',
  };
}

export async function runValidator(): Promise<void> {
  const inputDir = path.join(process.cwd(), 'shared', 'input');
  const outputDir = path.join(process.cwd(), 'shared', 'output');

  try {
    // Ensure output directory exists
    await mkdir(outputDir, { recursive: true });

    // Read all transaction files from input directory
    const files = await readdir(inputDir);
    const jsonFiles = files.filter((f) => f.endsWith('.json'));

    console.log(`[${getISOTimestamp()}] [transaction_validator] Starting validation of ${jsonFiles.length} transactions`);

    for (const file of jsonFiles) {
      const inputPath = path.join(inputDir, file);
      const outputPath = path.join(outputDir, file);

      try {
        // Read transaction
        const content = await readFile(inputPath, { encoding: 'utf8' });
        const message: Message<TransactionData> = JSON.parse(content);
        const transaction: TransactionData = message.data;

        // Validate
        const result = processMessage(transaction);

        // Log decision
        const logMessage = result.status === 'validated'
          ? `[${getISOTimestamp()}] [transaction_validator] ${result.transaction_id} → VALIDATED`
          : `[${getISOTimestamp()}] [transaction_validator] ${result.transaction_id} → REJECTED (${result.rejection_reason})`;
        console.log(logMessage);

        // Write result
        const outputMessage: Message<ValidationResult> = {
          message_id: uuidv4(),
          timestamp: getISOTimestamp(),
          source_agent: 'transaction_validator',
          target_agent: 'fraud_detector',
          message_type: 'transaction',
          data: result,
        };

        await writeFile(outputPath, JSON.stringify(outputMessage, null, 2));
      } catch (error) {
        console.error(`Error processing ${file}:`, error instanceof Error ? error.message : error);
      }
    }

    console.log(`[${getISOTimestamp()}] [transaction_validator] Validation complete`);
  } catch (error) {
    console.error('Validator error:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

// Run if called directly
/* istanbul ignore if */
if (require.main === module) {
  runValidator();
}

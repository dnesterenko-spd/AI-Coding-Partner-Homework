import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { Decimal } from 'decimal.js';
import { v4 as uuidv4 } from 'uuid';
import {
  ValidationResult,
  ScoredResult,
  Message,
  maskAccount,
  getISOTimestamp,
} from '../types';

interface ScoredMessage extends Message {
  data: ScoredResult;
}

export function processMessage(message: Message<ValidationResult>): ScoredResult {
  const data = message.data;

  // Pass through rejected transactions
  if (data.status === 'rejected') {
    return {
      ...data,
      fraud_risk_score: 0,
      fraud_risk_level: 'N/A',
    };
  }

  let score = 0;
  const amount = new Decimal(data.amount);

  // Scoring rules
  if (amount.greaterThan(new Decimal('50000'))) {
    score += 7; // +7 for amounts > $50,000
  } else if (amount.greaterThan(new Decimal('10000'))) {
    score += 3; // +3 for amounts > $10,000
  }

  // Check unusual hour (02:00-04:59 UTC)
  const timestamp = new Date(data.timestamp);
  const hour = timestamp.getUTCHours();
  if (hour >= 2 && hour < 5) {
    score += 2;
  }

  // Cross-border check
  const destCountry = data.metadata.country;
  if (destCountry !== 'US') {
    score += 1;
  }

  // Determine risk level
  let riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  if (score >= 7) {
    riskLevel = 'HIGH';
  } else if (score >= 3) {
    riskLevel = 'MEDIUM';
  } else {
    riskLevel = 'LOW';
  }

  const result: ScoredResult = {
    ...data,
    fraud_risk_score: score,
    fraud_risk_level: riskLevel,
  };

  console.log(
    `[${getISOTimestamp()}] [fraud_detector] ${data.transaction_id} (${amount}) → score: ${score}, risk: ${riskLevel}`
  );

  return result;
}

export async function runFraudDetector(): Promise<void> {
  const outputDir = path.join(process.cwd(), 'shared', 'output');
  const processingDir = path.join(process.cwd(), 'shared', 'processing');

  try {
    // Ensure processing directory exists
    await mkdir(processingDir, { recursive: true });

    // Read all validated messages from output directory
    const files = await readdir(outputDir);
    const jsonFiles = files.filter((f) => f.endsWith('.json'));

    console.log(`[${getISOTimestamp()}] [fraud_detector] Starting fraud detection for ${jsonFiles.length} transactions`);

    for (const file of jsonFiles) {
      const inputPath = path.join(outputDir, file);
      const outputPath = path.join(processingDir, file);

      try {
        // Read validated message
        const content = await readFile(inputPath, { encoding: 'utf8' });
        const message: Message<ValidationResult> = JSON.parse(content);

        // Process
        const result = processMessage(message);

        // Write scored message
        const scoredMessage: ScoredMessage = {
          message_id: uuidv4(),
          timestamp: getISOTimestamp(),
          source_agent: 'fraud_detector',
          target_agent: 'reporting_agent',
          message_type: 'transaction',
          data: result,
        };

        await writeFile(outputPath, JSON.stringify(scoredMessage, null, 2));
      } catch (error) {
        console.error(`Error processing ${file}:`, error instanceof Error ? error.message : error);
      }
    }

    console.log(`[${getISOTimestamp()}] [fraud_detector] Fraud detection complete`);
  } catch (error) {
    console.error('Fraud detector error:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

// Run if called directly
/* istanbul ignore if */
if (require.main === module) {
  runFraudDetector();
}

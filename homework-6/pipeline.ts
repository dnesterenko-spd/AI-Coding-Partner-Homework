import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import path from 'node:path';
import { v4 as uuidv4 } from 'uuid';
import { runValidator } from './agents/transaction_validator';
import { runFraudDetector } from './agents/fraud_detector';
import { runReportingAgent } from './agents/reporting_agent';
import { TransactionData, Message, getISOTimestamp } from './types';

async function setupDirectories(): Promise<void> {
  const dirs = [
    path.join(process.cwd(), 'shared', 'input'),
    path.join(process.cwd(), 'shared', 'output'),
    path.join(process.cwd(), 'shared', 'processing'),
    path.join(process.cwd(), 'shared', 'results'),
    path.join(process.cwd(), 'shared', 'logs'),
  ];

  for (const dir of dirs) {
    await mkdir(dir, { recursive: true });
  }

  console.log(`[${getISOTimestamp()}] [orchestrator] Directories initialized`);
}

async function loadTransactions(): Promise<void> {
  const transactionsPath = path.join(process.cwd(), 'sample-transactions.json');
  const inputDir = path.join(process.cwd(), 'shared', 'input');

  try {
    const content = await readFile(transactionsPath, { encoding: 'utf8' });
    const transactions: TransactionData[] = JSON.parse(content);

    console.log(`[${getISOTimestamp()}] [orchestrator] Loading ${transactions.length} transactions`);

    for (const transaction of transactions) {
      const message: Message<TransactionData> = {
        message_id: uuidv4(),
        timestamp: getISOTimestamp(),
        source_agent: 'orchestrator',
        target_agent: 'transaction_validator',
        message_type: 'transaction',
        data: transaction,
      };

      const outputPath = path.join(inputDir, `${transaction.transaction_id}.json`);
      await writeFile(outputPath, JSON.stringify(message, null, 2));
    }

    console.log(`[${getISOTimestamp()}] [orchestrator] All transactions loaded to shared/input/`);
  } catch (error) {
    console.error('Error loading transactions:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

async function runPipeline(): Promise<void> {
  console.log(`[${getISOTimestamp()}] [orchestrator] === STARTING BANKING TRANSACTION PIPELINE ===\n`);

  try {
    // Setup
    await setupDirectories();
    await loadTransactions();

    // Run agents in sequence
    console.log(`\n[${getISOTimestamp()}] [orchestrator] Running Transaction Validator...\n`);
    await runValidator();

    console.log(`\n[${getISOTimestamp()}] [orchestrator] Running Fraud Detector...\n`);
    await runFraudDetector();

    console.log(`\n[${getISOTimestamp()}] [orchestrator] Running Reporting Agent...\n`);
    await runReportingAgent();

    console.log(`\n[${getISOTimestamp()}] [orchestrator] === PIPELINE COMPLETE ===`);
  } catch (error) {
    console.error('Pipeline error:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

// Run pipeline
runPipeline();

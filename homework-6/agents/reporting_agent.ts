import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { v4 as uuidv4 } from 'uuid';
import {
  ScoredResult,
  FinalResult,
  PipelineSummary,
  Message,
  maskAccount,
  getISOTimestamp,
} from '../types';

export function processMessage(message: Message<ScoredResult>): FinalResult {
  return {
    ...message.data,
    processed_at: getISOTimestamp(),
  };
}

export async function runReportingAgent(): Promise<void> {
  const processingDir = path.join(process.cwd(), 'shared', 'processing');
  const resultsDir = path.join(process.cwd(), 'shared', 'results');

  try {
    // Ensure results directory exists
    await mkdir(resultsDir, { recursive: true });

    // Read all scored messages from processing directory
    const files = await readdir(processingDir);
    const jsonFiles = files.filter((f) => f.endsWith('.json'));

    console.log(`[${getISOTimestamp()}] [reporting_agent] Generating results for ${jsonFiles.length} transactions`);

    const results: FinalResult[] = [];
    const rejectionBreakdown: Record<string, number> = {};
    const riskDistribution: Record<string, number> = {};

    for (const file of jsonFiles) {
      const inputPath = path.join(processingDir, file);
      const resultPath = path.join(resultsDir, file);

      try {
        // Read scored message
        const content = await readFile(inputPath, { encoding: 'utf8' });
        const message: Message<ScoredResult> = JSON.parse(content);

        // Process
        const result = processMessage(message);
        results.push(result);

        // Track rejection reasons
        if (result.status === 'rejected' && result.rejection_reason) {
          rejectionBreakdown[result.rejection_reason] = (rejectionBreakdown[result.rejection_reason] || 0) + 1;
        }

        // Track risk distribution
        const riskLevel = result.fraud_risk_level;
        riskDistribution[riskLevel] = (riskDistribution[riskLevel] || 0) + 1;

        // Write individual result file
        const resultMessage: Message<FinalResult> = {
          message_id: uuidv4(),
          timestamp: getISOTimestamp(),
          source_agent: 'reporting_agent',
          target_agent: 'none',
          message_type: 'result',
          data: result,
        };

        await writeFile(resultPath, JSON.stringify(resultMessage, null, 2));

        console.log(`[${getISOTimestamp()}] [reporting_agent] ${result.transaction_id} → ${result.status.toUpperCase()}`);
      } catch (error) {
        console.error(`Error processing ${file}:`, error instanceof Error ? error.message : error);
      }
    }

    // Generate summary
    const acceptedCount = results.filter((r) => r.status === 'validated').length;
    const rejectedCount = results.filter((r) => r.status === 'rejected').length;

    const summary: PipelineSummary = {
      total_transactions: results.length,
      accepted_count: acceptedCount,
      rejected_count: rejectedCount,
      rejection_breakdown: rejectionBreakdown,
      risk_distribution: riskDistribution,
      generated_at: getISOTimestamp(),
    };

    // Write summary
    const summaryPath = path.join(resultsDir, 'pipeline_summary.json');
    await writeFile(summaryPath, JSON.stringify(summary, null, 2));

    // Console summary
    console.log(`[${getISOTimestamp()}] [reporting_agent] === PIPELINE SUMMARY ===`);
    console.log(`Total transactions: ${summary.total_transactions}`);
    console.log(`Accepted: ${summary.accepted_count}`);
    console.log(`Rejected: ${summary.rejected_count}`);
    console.log(`Rejection breakdown:`, summary.rejection_breakdown);
    console.log(`Risk distribution:`, summary.risk_distribution);
    console.log(`Generated at: ${summary.generated_at}`);
  } catch (error) {
    console.error('Reporting agent error:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  runReportingAgent();
}

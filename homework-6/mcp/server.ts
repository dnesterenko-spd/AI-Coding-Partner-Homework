#!/usr/bin/env node

import { FastMCP } from 'fastmcp';
import { z } from 'zod';
import * as fs from 'fs';
import * as path from 'path';
import { FinalResult, PipelineSummary } from '../types';

// Initialize FastMCP
const server = new FastMCP({
  name: 'pipeline-status',
  version: '1.0.0'
});

// Helper function to read results from shared/results/
function readResultFiles(): FinalResult[] {
  const resultsDir = path.join(__dirname, '..', 'shared', 'results');
  const results: FinalResult[] = [];

  if (!fs.existsSync(resultsDir)) {
    return results;
  }

  const files = fs.readdirSync(resultsDir);
  for (const file of files) {
    if (file.endsWith('.json') && !file.includes('summary')) {
      try {
        const content = fs.readFileSync(path.join(resultsDir, file), 'utf-8');
        const message = JSON.parse(content);
        // Extract the data from the message wrapper
        if (message.data && message.data.transaction_id) {
          results.push(message.data as FinalResult);
        }
      } catch (error) {
        console.error(`Error reading ${file}:`, error);
      }
    }
  }

  return results;
}

// Helper function to generate pipeline summary
function generatePipelineSummary(): PipelineSummary {
  const results = readResultFiles();

  const summary: PipelineSummary = {
    total_transactions: results.length,
    accepted_count: 0,
    rejected_count: 0,
    rejection_breakdown: {},
    risk_distribution: {
      LOW: 0,
      MEDIUM: 0,
      HIGH: 0,
      'N/A': 0
    },
    generated_at: new Date().toISOString()
  };

  for (const result of results) {
    if (result.status === 'validated') {
      summary.accepted_count++;
      summary.risk_distribution[result.fraud_risk_level]++;
    } else {
      summary.rejected_count++;
      const reason = result.rejection_reason || 'Unknown';
      summary.rejection_breakdown[reason] = (summary.rejection_breakdown[reason] || 0) + 1;
    }
  }

  return summary;
}

// Tool: get_transaction_status
server.addTool({
  name: 'get_transaction_status',
  description: 'Get the status of a specific transaction by ID',
  parameters: z.object({
    transaction_id: z.string().describe('The transaction ID to look up')
  }),
  execute: async (args) => {
    const results = readResultFiles();
    const transaction = results.find(r => r.transaction_id === args.transaction_id);

    if (!transaction) {
      return `Transaction ${args.transaction_id} not found in results`;
    }

    const response = {
      transaction_id: transaction.transaction_id,
      status: transaction.status,
      fraud_risk_level: transaction.fraud_risk_level,
      fraud_risk_score: transaction.fraud_risk_score,
      rejection_reason: transaction.rejection_reason,
      processed_at: transaction.processed_at,
      amount: transaction.amount,
      currency: transaction.currency
    };

    return JSON.stringify(response, null, 2);
  }
});

// Tool: list_pipeline_results
server.addTool({
  name: 'list_pipeline_results',
  description: 'Get a summary of all processed transactions',
  parameters: z.object({}),
  execute: async () => {
    const summary = generatePipelineSummary();
    const results = readResultFiles();

    // Add a brief list of transactions
    const transactions = results.map(r => ({
      id: r.transaction_id,
      status: r.status,
      risk: r.fraud_risk_level,
      amount: `${r.amount} ${r.currency}`
    }));

    const response = {
      summary,
      transactions: transactions.slice(0, 20), // Limit to first 20 for brevity
      total_count: results.length
    };

    return JSON.stringify(response, null, 2);
  }
});

// Resource: pipeline://summary
server.addResource({
  name: 'Pipeline Summary',
  description: 'Latest pipeline run summary',
  uri: 'pipeline://summary',
  mimeType: 'text/plain',
  load: async () => {
    const summary = generatePipelineSummary();

    let text = `=== Pipeline Summary ===\n`;
    text += `Generated at: ${summary.generated_at}\n\n`;
    text += `Total Transactions: ${summary.total_transactions}\n`;
    text += `Accepted: ${summary.accepted_count}\n`;
    text += `Rejected: ${summary.rejected_count}\n\n`;

    if (Object.keys(summary.rejection_breakdown).length > 0) {
      text += `Rejection Reasons:\n`;
      for (const [reason, count] of Object.entries(summary.rejection_breakdown)) {
        text += `  - ${reason}: ${count}\n`;
      }
      text += '\n';
    }

    text += `Risk Distribution:\n`;
    for (const [level, count] of Object.entries(summary.risk_distribution)) {
      text += `  - ${level}: ${count}\n`;
    }

    return {
      text,
      uri: 'pipeline://summary',
      mimeType: 'text/plain'
    };
  }
});

// Start the server with stdio transport
server.start({
  transportType: 'stdio'
});
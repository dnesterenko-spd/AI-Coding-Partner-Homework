import { readFile, writeFile, mkdir, rm } from 'node:fs/promises';
import path from 'node:path';
import { v4 as uuidv4 } from 'uuid';
import { processMessage as validateMessage } from '../agents/transaction_validator';
import { processMessage as scoreMessage } from '../agents/fraud_detector';
import { processMessage as reportMessage } from '../agents/reporting_agent';
import { TransactionData, ValidationResult, Message } from '../types';

describe('Pipeline Integration Tests', () => {
  const testTransaction: TransactionData = {
    transaction_id: 'TXN-INT-001',
    timestamp: '2026-03-16T09:00:00Z',
    source_account: 'ACC-TEST-001',
    destination_account: 'ACC-TEST-002',
    amount: '5000.00',
    currency: 'USD',
    transaction_type: 'transfer',
    description: 'Test transaction',
    metadata: {
      channel: 'online',
      country: 'US',
    },
  };

  test('complete transaction flow: validator -> fraud detector -> reporter', () => {
    // Step 1: Validate transaction
    const validated = validateMessage(testTransaction);
    expect(validated.status).toBe('validated');
    expect(validated.transaction_id).toBe('TXN-INT-001');

    // Step 2: Score for fraud (wrap in message for detector)
    const scoreMsg: Message<ValidationResult> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: validated,
    };
    const scored = scoreMessage(scoreMsg);
    expect(scored.fraud_risk_level).toBe('LOW');
    expect(scored.fraud_risk_score).toBe(0);

    // Step 3: Generate report (wrap in message for reporter)
    const reportMsg: Message<any> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'fraud_detector',
      target_agent: 'reporting_agent',
      message_type: 'transaction',
      data: scored,
    };
    const reported = reportMessage(reportMsg);
    expect(reported.processed_at).toBeDefined();
    expect(reported.transaction_id).toBe('TXN-INT-001');
  });

  test('invalid currency should be rejected at validation stage', () => {
    const invalidTxn: TransactionData = {
      ...testTransaction,
      transaction_id: 'TXN-INT-002',
      currency: 'INVALID',
    };

    const result = validateMessage(invalidTxn);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('INVALID_CURRENCY');

    // Rejected transaction should pass through fraud detector with N/A risk
    const scoreMsg: Message<ValidationResult> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: result,
    };
    const scored = scoreMessage(scoreMsg);
    expect(scored.fraud_risk_level).toBe('N/A');
    expect(scored.fraud_risk_score).toBe(0);
  });

  test('high-value transaction should be flagged at fraud detection stage', () => {
    const highValueTxn: TransactionData = {
      ...testTransaction,
      transaction_id: 'TXN-INT-003',
      amount: '75000.00',
    };

    const validated = validateMessage(highValueTxn);
    expect(validated.status).toBe('validated');

    const scoreMsg: Message<ValidationResult> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: validated,
    };
    const scored = scoreMessage(scoreMsg);
    expect(scored.fraud_risk_level).toBe('HIGH');
    expect(scored.fraud_risk_score).toBe(7);
  });

  test('negative amount should be rejected at validation stage', () => {
    const negativeAmountTxn: TransactionData = {
      ...testTransaction,
      transaction_id: 'TXN-INT-004',
      amount: '-100.00',
    };

    const result = validateMessage(negativeAmountTxn);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('NEGATIVE_AMOUNT');
  });

  test('missing required field should be rejected', () => {
    const missingFieldTxn = {
      ...testTransaction,
      amount: '',
    };

    const result = validateMessage(missingFieldTxn as TransactionData);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('cross-border transaction should add fraud score points', () => {
    const crossBorderTxn: TransactionData = {
      ...testTransaction,
      transaction_id: 'TXN-INT-005',
      amount: '500.00',
      currency: 'EUR',
      metadata: {
        channel: 'api',
        country: 'DE',
      },
    };

    const validated = validateMessage(crossBorderTxn);
    expect(validated.status).toBe('validated');

    const scoreMsg: Message<ValidationResult> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: validated,
    };
    const scored = scoreMessage(scoreMsg);
    // Cross-border adds 1 point
    expect(scored.fraud_risk_score).toBe(1);
    expect(scored.fraud_risk_level).toBe('LOW');
  });

  test('unusual hour transaction should add fraud score points', () => {
    const unusualHourTxn: TransactionData = {
      ...testTransaction,
      transaction_id: 'TXN-INT-006',
      timestamp: '2026-03-16T03:00:00Z', // 3am UTC
      amount: '500.00',
    };

    const validated = validateMessage(unusualHourTxn);
    expect(validated.status).toBe('validated');

    const scoreMsg: Message<ValidationResult> = {
      message_id: uuidv4(),
      timestamp: new Date().toISOString(),
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: validated,
    };
    const scored = scoreMessage(scoreMsg);
    // Unusual hour (02-04 UTC) adds 2 points
    expect(scored.fraud_risk_score).toBe(2);
    expect(scored.fraud_risk_level).toBe('LOW');
  });
});

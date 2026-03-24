import { processMessage } from '../agents/fraud_detector';
import { ValidationResult, Message } from '../types';

describe('Fraud Detector', () => {
  const baseValidTransaction: ValidationResult = {
    transaction_id: 'TXN001',
    timestamp: '2026-03-16T09:00:00Z',
    source_account: 'ACC-1001',
    destination_account: 'ACC-2001',
    amount: '1500.00',
    currency: 'USD',
    transaction_type: 'transfer',
    description: 'Monthly rent payment',
    metadata: {
      channel: 'online',
      country: 'US',
    },
    status: 'validated',
  };

  const createMessage = (data: ValidationResult): Message<ValidationResult> => ({
    message_id: '123',
    timestamp: new Date().toISOString(),
    source_agent: 'transaction_validator',
    target_agent: 'fraud_detector',
    message_type: 'transaction',
    data,
  });

  test('should assign LOW risk to small transactions', () => {
    const message = createMessage(baseValidTransaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('LOW');
    expect(result.fraud_risk_score).toBeLessThan(3);
  });

  test('should assign MEDIUM risk to $25,000 transactions (TXN002)', () => {
    const transaction = {
      ...baseValidTransaction,
      transaction_id: 'TXN002',
      amount: '25000.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('MEDIUM');
    expect(result.fraud_risk_score).toBeGreaterThanOrEqual(3);
    expect(result.fraud_risk_score).toBeLessThan(7);
  });

  test('should assign HIGH risk to $75,000 transactions (TXN005)', () => {
    const transaction = {
      ...baseValidTransaction,
      transaction_id: 'TXN005',
      amount: '75000.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('HIGH');
    expect(result.fraud_risk_score).toBeGreaterThanOrEqual(7);
  });

  test('should add points for unusual hour (02:47 UTC)', () => {
    const transaction = {
      ...baseValidTransaction,
      transaction_id: 'TXN004',
      timestamp: '2026-03-16T02:47:00Z',
      amount: '500.00',
      currency: 'EUR',
      metadata: {
        channel: 'api',
        country: 'DE',
      },
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    // Score should include +2 for unusual hour and +1 for cross-border
    expect(result.fraud_risk_score).toBeGreaterThanOrEqual(3);
  });

  test('should add points for cross-border transactions', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '200.00',
      currency: 'EUR',
      metadata: {
        channel: 'api',
        country: 'DE',
      },
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    // Score should include +1 for cross-border
    expect(result.fraud_risk_score).toBeGreaterThanOrEqual(1);
  });

  test('should pass through rejected transactions with N/A risk', () => {
    const transaction: ValidationResult = {
      ...baseValidTransaction,
      transaction_id: 'TXN006',
      status: 'rejected',
      rejection_reason: 'INVALID_CURRENCY',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.status).toBe('rejected');
    expect(result.fraud_risk_level).toBe('N/A');
    expect(result.fraud_risk_score).toBe(0);
  });

  test('should correctly score $10,001 transaction as MEDIUM', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '10001.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(3);
    expect(result.fraud_risk_level).toBe('MEDIUM');
  });

  test('should correctly score $50,001 transaction as HIGH', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '50001.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(7);
    expect(result.fraud_risk_level).toBe('HIGH');
  });

  test('should add 2 points for 02:00 UTC hour', () => {
    const transaction = {
      ...baseValidTransaction,
      timestamp: '2026-03-16T02:00:00Z',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBeGreaterThanOrEqual(2);
  });

  test('should not add points for 05:00 UTC hour (outside window)', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '100.00',
      timestamp: '2026-03-16T05:00:00Z',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(0);
  });

  test('should not add points for US transactions', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '100.00',
      metadata: {
        channel: 'online',
        country: 'US',
      },
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(0);
    expect(result.fraud_risk_level).toBe('LOW');
  });

  test('should preserve all data in result', () => {
    const message = createMessage(baseValidTransaction);
    const result = processMessage(message);
    expect(result.transaction_id).toBe(baseValidTransaction.transaction_id);
    expect(result.amount).toBe(baseValidTransaction.amount);
    expect(result.status).toBe(baseValidTransaction.status);
  });

  test('should cap score at 10', () => {
    // High amount + unusual time + cross-border
    const transaction = {
      ...baseValidTransaction,
      amount: '100000.00',
      timestamp: '2026-03-16T03:00:00Z',
      metadata: {
        channel: 'api',
        country: 'CH',
      },
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBeLessThanOrEqual(10);
  });

  test('should handle edge case at exactly $10,000', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '10000.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(0);
    expect(result.fraud_risk_level).toBe('LOW');
  });

  test('should handle edge case at exactly $50,000', () => {
    const transaction = {
      ...baseValidTransaction,
      amount: '50000.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(3);
    expect(result.fraud_risk_level).toBe('MEDIUM');
  });

  test('should not add points for hour at boundary 05:00 UTC', () => {
    const transaction = {
      ...baseValidTransaction,
      timestamp: '2026-03-16T05:00:00Z',
      amount: '100.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(0);
  });

  test('should add points for hour at boundary 04:59 UTC', () => {
    const transaction = {
      ...baseValidTransaction,
      timestamp: '2026-03-16T04:59:00Z',
      amount: '100.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(2);
  });

  test('should not add points for hour at boundary 01:59 UTC', () => {
    const transaction = {
      ...baseValidTransaction,
      timestamp: '2026-03-16T01:59:00Z',
      amount: '100.00',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(0);
  });
});

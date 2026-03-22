import { processMessage } from '../agents/reporting_agent';
import { ScoredResult, Message } from '../types';

describe('Reporting Agent', () => {
  const baseTransaction: ScoredResult = {
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
    fraud_risk_score: 0,
    fraud_risk_level: 'LOW',
  };

  const createMessage = (data: ScoredResult): Message<ScoredResult> => ({
    message_id: '123',
    timestamp: new Date().toISOString(),
    source_agent: 'fraud_detector',
    target_agent: 'reporting_agent',
    message_type: 'transaction',
    data,
  });

  test('should add processed_at timestamp to result', () => {
    const message = createMessage(baseTransaction);
    const result = processMessage(message);
    expect(result.processed_at).toBeDefined();
    expect(result.processed_at).toMatch(/^\d{4}-\d{2}-\d{2}T/); // ISO 8601 format
  });

  test('should include accepted status in result', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      status: 'validated',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.status).toBe('validated');
  });

  test('should include rejected status with reason in result', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      transaction_id: 'TXN006',
      status: 'rejected',
      rejection_reason: 'INVALID_CURRENCY',
      fraud_risk_level: 'N/A',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('INVALID_CURRENCY');
  });

  test('should preserve fraud risk score and level', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      fraud_risk_score: 5,
      fraud_risk_level: 'MEDIUM',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_score).toBe(5);
    expect(result.fraud_risk_level).toBe('MEDIUM');
  });

  test('should preserve all transaction data', () => {
    const message = createMessage(baseTransaction);
    const result = processMessage(message);
    expect(result.transaction_id).toBe(baseTransaction.transaction_id);
    expect(result.amount).toBe(baseTransaction.amount);
    expect(result.currency).toBe(baseTransaction.currency);
    expect(result.source_account).toBe(baseTransaction.source_account);
    expect(result.destination_account).toBe(baseTransaction.destination_account);
    expect(result.metadata).toEqual(baseTransaction.metadata);
  });

  test('should handle HIGH risk transactions', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      transaction_id: 'TXN005',
      amount: '75000.00',
      fraud_risk_score: 7,
      fraud_risk_level: 'HIGH',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('HIGH');
  });

  test('should handle N/A risk level for rejected transactions', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      transaction_id: 'TXN007',
      status: 'rejected',
      rejection_reason: 'NEGATIVE_AMOUNT',
      fraud_risk_score: 0,
      fraud_risk_level: 'N/A',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('N/A');
  });

  test('should not modify transaction data except adding processed_at', () => {
    const message = createMessage(baseTransaction);
    const result = processMessage(message);
    const { processed_at, ...rest } = result;
    expect(rest).toEqual(baseTransaction);
    expect(processed_at).toBeDefined();
  });

  test('should generate ISO 8601 timestamp', () => {
    const message = createMessage(baseTransaction);
    const result = processMessage(message);
    const timestamp = new Date(result.processed_at);
    expect(timestamp.getTime()).toBeGreaterThan(0);
  });

  test('should handle rejected transaction without rejection_reason', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      status: 'rejected',
      fraud_risk_level: 'N/A',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.status).toBe('rejected');
    expect(result.processed_at).toBeDefined();
  });

  test('should handle MEDIUM risk level', () => {
    const transaction: ScoredResult = {
      ...baseTransaction,
      fraud_risk_score: 5,
      fraud_risk_level: 'MEDIUM',
    };
    const message = createMessage(transaction);
    const result = processMessage(message);
    expect(result.fraud_risk_level).toBe('MEDIUM');
    expect(result.fraud_risk_score).toBe(5);
  });
});

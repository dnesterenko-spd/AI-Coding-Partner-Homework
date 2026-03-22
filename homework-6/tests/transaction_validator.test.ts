import { processMessage } from '../agents/transaction_validator';
import { TransactionData } from '../types';

describe('Transaction Validator', () => {
  const validTransaction: TransactionData = {
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
  };

  test('should validate a valid transaction', () => {
    const result = processMessage(validTransaction);
    expect(result.status).toBe('validated');
    expect(result.rejection_reason).toBeUndefined();
  });

  test('should reject transactions with negative amounts', () => {
    const transaction = {
      ...validTransaction,
      transaction_id: 'TXN007',
      amount: '-100.00',
    };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('NEGATIVE_AMOUNT');
  });

  test('should reject transactions with invalid currency (TXN006)', () => {
    const transaction = {
      ...validTransaction,
      transaction_id: 'TXN006',
      currency: 'XYZ',
    };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('INVALID_CURRENCY');
  });

  test('should accept valid currencies', () => {
    const currencies = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY'];
    for (const currency of currencies) {
      const transaction = { ...validTransaction, currency };
      const result = processMessage(transaction);
      expect(result.status).toBe('validated');
    }
  });

  test('should reject transactions with missing transaction_id', () => {
    const transaction = { ...validTransaction, transaction_id: '' };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject transactions with missing amount', () => {
    const transaction = { ...validTransaction, amount: '' };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject transactions with missing currency', () => {
    const transaction = { ...validTransaction, currency: '' };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject transactions with missing source_account', () => {
    const transaction = { ...validTransaction, source_account: '' };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject transactions with missing destination_account', () => {
    const transaction = { ...validTransaction, destination_account: '' };
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should handle zero amount as valid', () => {
    const transaction = { ...validTransaction, amount: '0.00' };
    const result = processMessage(transaction);
    expect(result.status).toBe('validated');
  });

  test('should preserve all transaction data in result', () => {
    const result = processMessage(validTransaction);
    expect(result.transaction_id).toBe(validTransaction.transaction_id);
    expect(result.amount).toBe(validTransaction.amount);
    expect(result.currency).toBe(validTransaction.currency);
    expect(result.source_account).toBe(validTransaction.source_account);
    expect(result.destination_account).toBe(validTransaction.destination_account);
  });

  test('should handle large valid amounts', () => {
    const transaction = { ...validTransaction, amount: '999999999.99' };
    const result = processMessage(transaction);
    expect(result.status).toBe('validated');
  });

  test('should be case-insensitive for currency codes', () => {
    const transaction = { ...validTransaction, currency: 'usd' };
    const result = processMessage(transaction);
    // Note: the actual implementation converts to uppercase, so this should validate
    expect(result.status).toBe('validated');
  });

  test('should reject when transaction_id is undefined', () => {
    const transaction: any = { ...validTransaction };
    delete transaction.transaction_id;
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject when amount is undefined', () => {
    const transaction: any = { ...validTransaction };
    delete transaction.amount;
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject when currency is undefined', () => {
    const transaction: any = { ...validTransaction };
    delete transaction.currency;
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject when source_account is undefined', () => {
    const transaction: any = { ...validTransaction };
    delete transaction.source_account;
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should reject when destination_account is undefined', () => {
    const transaction: any = { ...validTransaction };
    delete transaction.destination_account;
    const result = processMessage(transaction);
    expect(result.status).toBe('rejected');
    expect(result.rejection_reason).toBe('MISSING_FIELD');
  });

  test('should validate lowercase currency', () => {
    const transaction = { ...validTransaction, currency: 'eur' };
    const result = processMessage(transaction);
    expect(result.status).toBe('validated');
  });

  test('should validate mixed case currency', () => {
    const transaction = { ...validTransaction, currency: 'GbP' };
    const result = processMessage(transaction);
    expect(result.status).toBe('validated');
  });
});

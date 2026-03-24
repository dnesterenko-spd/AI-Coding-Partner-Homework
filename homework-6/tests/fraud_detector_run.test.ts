import { runFraudDetector } from '../agents/fraud_detector';
import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';

// Mock node:fs/promises
jest.mock('node:fs/promises');

const mockedReaddir = readdir as jest.MockedFunction<typeof readdir>;
const mockedReadFile = readFile as jest.MockedFunction<typeof readFile>;
const mockedWriteFile = writeFile as jest.MockedFunction<typeof writeFile>;
const mockedMkdir = mkdir as jest.MockedFunction<typeof mkdir>;

describe('Fraud Detector - runFraudDetector', () => {
  const outputDir = path.join(process.cwd(), 'shared', 'output');
  const processingDir = path.join(process.cwd(), 'shared', 'processing');

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'log').mockImplementation();
    jest.spyOn(console, 'error').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('should score validated transactions and write results', async () => {
    const validatedMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN001',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '1500.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'validated',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json', 'not-json.txt'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validatedMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    // Verify mkdir was called
    expect(mockedMkdir).toHaveBeenCalledWith(processingDir, { recursive: true });

    // Verify readdir was called
    expect(mockedReaddir).toHaveBeenCalledWith(outputDir);

    // Verify readFile was called for JSON files only
    expect(mockedReadFile).toHaveBeenCalledTimes(1);

    // Verify writeFile was called with scored result
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.fraud_risk_score).toBeDefined();
    expect(result.data.fraud_risk_level).toBeDefined();
    expect(result.source_agent).toBe('fraud_detector');
    expect(result.target_agent).toBe('reporting_agent');
  });

  test('should handle rejected transactions', async () => {
    const rejectedMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN002',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '-100.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'rejected',
        rejection_reason: 'NEGATIVE_AMOUNT',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(rejectedMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    // Verify writeFile was called with N/A risk level
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.fraud_risk_level).toBe('N/A');
    expect(result.data.fraud_risk_score).toBe(0);
  });

  test('should handle empty output directory', async () => {
    mockedReaddir.mockResolvedValue([] as any);
    mockedMkdir.mockResolvedValue(undefined);

    await runFraudDetector();

    expect(mockedReaddir).toHaveBeenCalledWith(outputDir);
    expect(mockedReadFile).not.toHaveBeenCalled();
    expect(mockedWriteFile).not.toHaveBeenCalled();
  });

  test('should handle file read errors gracefully', async () => {
    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json'] as any);
    mockedReadFile
      .mockRejectedValueOnce(new Error('File read error'))
      .mockResolvedValueOnce(JSON.stringify({
        message_id: 'msg2',
        timestamp: '2026-03-22T10:01:00Z',
        source_agent: 'transaction_validator',
        target_agent: 'fraud_detector',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN002',
          timestamp: '2026-03-16T09:05:00Z',
          source_account: 'ACC-1002',
          destination_account: 'ACC-2002',
          amount: '100.00',
          currency: 'USD',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'validated',
        },
      }));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    // Should log error but continue
    expect(console.error).toHaveBeenCalled();

    // Should still process the second file
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
  });

  test('should handle malformed JSON gracefully', async () => {
    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue('invalid json');
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    // Should log error
    expect(console.error).toHaveBeenCalled();
    expect(mockedWriteFile).not.toHaveBeenCalled();
  });

  test('should exit with code 1 on directory read error', async () => {
    mockedReaddir.mockRejectedValue(new Error('Directory not found'));
    mockedMkdir.mockResolvedValue(undefined);

    const mockExit = jest.spyOn(process, 'exit').mockImplementation((code?: any) => {
      throw new Error(`process.exit called with ${code}`);
    });

    await expect(runFraudDetector()).rejects.toThrow('process.exit called with 1');

    expect(console.error).toHaveBeenCalledWith(
      'Fraud detector error:',
      'Directory not found'
    );
    expect(mockExit).toHaveBeenCalledWith(1);

    mockExit.mockRestore();
  });

  test('should process multiple transactions', async () => {
    const message1 = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN001',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '1500.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'validated',
      },
    };

    const message2 = {
      message_id: 'msg2',
      timestamp: '2026-03-22T10:01:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN002',
        timestamp: '2026-03-16T09:05:00Z',
        source_account: 'ACC-1002',
        destination_account: 'ACC-2002',
        amount: '75000.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'validated',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json'] as any);
    mockedReadFile
      .mockResolvedValueOnce(JSON.stringify(message1))
      .mockResolvedValueOnce(JSON.stringify(message2));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    expect(mockedWriteFile).toHaveBeenCalledTimes(2);

    const result1 = JSON.parse((mockedWriteFile.mock.calls as any)[0][1]);
    expect(result1.data.fraud_risk_level).toBe('LOW');

    const result2 = JSON.parse((mockedWriteFile.mock.calls as any)[1][1]);
    expect(result2.data.fraud_risk_level).toBe('HIGH');
  });

  test('should score high-value cross-border transaction at unusual hour', async () => {
    const message = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN003',
        timestamp: '2026-03-16T03:00:00Z', // Unusual hour
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '75000.00', // High value
        currency: 'EUR',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'DE' }, // Cross-border
        status: 'validated',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(message));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    // Should have: +7 (high value) +2 (unusual hour) +1 (cross-border) = 10
    expect(result.data.fraud_risk_score).toBe(10);
    expect(result.data.fraud_risk_level).toBe('HIGH');
  });

  test('should log fraud detection results', async () => {
    const validatedMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN001',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '1500.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'validated',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validatedMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runFraudDetector();

    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining('TXN001')
    );
    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining('fraud_detector')
    );
  });

  test('should handle writeFile errors gracefully', async () => {
    const validatedMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'transaction_validator',
      target_agent: 'fraud_detector',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN001',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '1500.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'validated',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validatedMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockRejectedValue(new Error('Write error'));

    await runFraudDetector();

    // Should log error but continue
    expect(console.error).toHaveBeenCalled();
  });
});

import { runReportingAgent } from '../agents/reporting_agent';
import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';

// Mock node:fs/promises
jest.mock('node:fs/promises');

const mockedReaddir = readdir as jest.MockedFunction<typeof readdir>;
const mockedReadFile = readFile as jest.MockedFunction<typeof readFile>;
const mockedWriteFile = writeFile as jest.MockedFunction<typeof writeFile>;
const mockedMkdir = mkdir as jest.MockedFunction<typeof mkdir>;

describe('Reporting Agent - runReportingAgent', () => {
  const processingDir = path.join(process.cwd(), 'shared', 'processing');
  const resultsDir = path.join(process.cwd(), 'shared', 'results');

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'log').mockImplementation();
    jest.spyOn(console, 'error').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('should process scored messages and generate summary', async () => {
    const scoredMessage1 = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'fraud_detector',
      target_agent: 'reporting_agent',
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
        fraud_risk_score: 0,
        fraud_risk_level: 'LOW',
      },
    };

    const scoredMessage2 = {
      message_id: 'msg2',
      timestamp: '2026-03-22T10:01:00Z',
      source_agent: 'fraud_detector',
      target_agent: 'reporting_agent',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN002',
        timestamp: '2026-03-16T09:05:00Z',
        source_account: 'ACC-1002',
        destination_account: 'ACC-2002',
        amount: '-100.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
        status: 'rejected',
        rejection_reason: 'NEGATIVE_AMOUNT',
        fraud_risk_score: 0,
        fraud_risk_level: 'N/A',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json', 'not-json.txt'] as any);
    mockedReadFile
      .mockResolvedValueOnce(JSON.stringify(scoredMessage1))
      .mockResolvedValueOnce(JSON.stringify(scoredMessage2));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    // Verify mkdir was called
    expect(mockedMkdir).toHaveBeenCalledWith(resultsDir, { recursive: true });

    // Verify readdir was called
    expect(mockedReaddir).toHaveBeenCalledWith(processingDir);

    // Verify readFile was called for JSON files only
    expect(mockedReadFile).toHaveBeenCalledTimes(2);

    // Verify writeFile was called for results and summary
    expect(mockedWriteFile).toHaveBeenCalledTimes(3); // 2 results + 1 summary

    // Check summary generation
    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    expect(summaryCall).toBeDefined();
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.total_transactions).toBe(2);
    expect(summary.accepted_count).toBe(1);
    expect(summary.rejected_count).toBe(1);
    expect(summary.rejection_breakdown).toEqual({ NEGATIVE_AMOUNT: 1 });
    expect(summary.risk_distribution).toEqual({ LOW: 1, 'N/A': 1 });
  });

  test('should handle empty processing directory', async () => {
    mockedReaddir.mockResolvedValue([] as any);
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    expect(mockedReaddir).toHaveBeenCalledWith(processingDir);
    expect(mockedReadFile).not.toHaveBeenCalled();

    // Should still write summary
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
    const summaryCall = (mockedWriteFile.mock.calls as any)[0];
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.total_transactions).toBe(0);
  });

  test('should handle file read errors gracefully', async () => {
    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json'] as any);
    mockedReadFile
      .mockRejectedValueOnce(new Error('File read error'))
      .mockResolvedValueOnce(JSON.stringify({
        message_id: 'msg2',
        timestamp: '2026-03-22T10:01:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
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
          fraud_risk_score: 0,
          fraud_risk_level: 'LOW',
        },
      }));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    // Should log error but continue
    expect(console.error).toHaveBeenCalled();

    // Should still process the second file
    expect(mockedWriteFile).toHaveBeenCalledWith(
      expect.stringContaining('txn2.json'),
      expect.any(String)
    );

    // Should write summary with only successful transaction
    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.total_transactions).toBe(1);
  });

  test('should handle malformed JSON gracefully', async () => {
    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue('invalid json');
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    // Should log error
    expect(console.error).toHaveBeenCalled();

    // Should write empty summary
    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.total_transactions).toBe(0);
  });

  test('should exit with code 1 on directory read error', async () => {
    mockedReaddir.mockRejectedValue(new Error('Directory not found'));
    mockedMkdir.mockResolvedValue(undefined);

    const mockExit = jest.spyOn(process, 'exit').mockImplementation((code?: any) => {
      throw new Error(`process.exit called with ${code}`);
    });

    await expect(runReportingAgent()).rejects.toThrow('process.exit called with 1');

    expect(console.error).toHaveBeenCalledWith(
      'Reporting agent error:',
      'Directory not found'
    );
    expect(mockExit).toHaveBeenCalledWith(1);

    mockExit.mockRestore();
  });

  test('should track multiple rejection reasons', async () => {
    const messages = [
      {
        message_id: 'msg1',
        timestamp: '2026-03-22T10:00:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN001',
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
          fraud_risk_score: 0,
          fraud_risk_level: 'N/A',
        },
      },
      {
        message_id: 'msg2',
        timestamp: '2026-03-22T10:01:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN002',
          timestamp: '2026-03-16T09:05:00Z',
          source_account: 'ACC-1002',
          destination_account: 'ACC-2002',
          amount: '100.00',
          currency: 'XYZ',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'rejected',
          rejection_reason: 'INVALID_CURRENCY',
          fraud_risk_score: 0,
          fraud_risk_level: 'N/A',
        },
      },
      {
        message_id: 'msg3',
        timestamp: '2026-03-22T10:02:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN003',
          timestamp: '2026-03-16T09:10:00Z',
          source_account: 'ACC-1003',
          destination_account: 'ACC-2003',
          amount: '-50.00',
          currency: 'USD',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'rejected',
          rejection_reason: 'NEGATIVE_AMOUNT',
          fraud_risk_score: 0,
          fraud_risk_level: 'N/A',
        },
      },
    ];

    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json', 'txn3.json'] as any);
    mockedReadFile
      .mockResolvedValueOnce(JSON.stringify(messages[0]))
      .mockResolvedValueOnce(JSON.stringify(messages[1]))
      .mockResolvedValueOnce(JSON.stringify(messages[2]));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.rejection_breakdown).toEqual({
      NEGATIVE_AMOUNT: 2,
      INVALID_CURRENCY: 1,
    });
  });

  test('should track risk distribution', async () => {
    const messages = [
      {
        message_id: 'msg1',
        timestamp: '2026-03-22T10:00:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN001',
          timestamp: '2026-03-16T09:00:00Z',
          source_account: 'ACC-1001',
          destination_account: 'ACC-2001',
          amount: '100.00',
          currency: 'USD',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'validated',
          fraud_risk_score: 0,
          fraud_risk_level: 'LOW',
        },
      },
      {
        message_id: 'msg2',
        timestamp: '2026-03-22T10:01:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN002',
          timestamp: '2026-03-16T09:05:00Z',
          source_account: 'ACC-1002',
          destination_account: 'ACC-2002',
          amount: '25000.00',
          currency: 'USD',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'validated',
          fraud_risk_score: 3,
          fraud_risk_level: 'MEDIUM',
        },
      },
      {
        message_id: 'msg3',
        timestamp: '2026-03-22T10:02:00Z',
        source_agent: 'fraud_detector',
        target_agent: 'reporting_agent',
        message_type: 'transaction',
        data: {
          transaction_id: 'TXN003',
          timestamp: '2026-03-16T09:10:00Z',
          source_account: 'ACC-1003',
          destination_account: 'ACC-2003',
          amount: '75000.00',
          currency: 'USD',
          transaction_type: 'transfer',
          description: 'Test',
          metadata: { channel: 'online', country: 'US' },
          status: 'validated',
          fraud_risk_score: 7,
          fraud_risk_level: 'HIGH',
        },
      },
    ];

    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json', 'txn3.json'] as any);
    mockedReadFile
      .mockResolvedValueOnce(JSON.stringify(messages[0]))
      .mockResolvedValueOnce(JSON.stringify(messages[1]))
      .mockResolvedValueOnce(JSON.stringify(messages[2]));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    const summary = JSON.parse(summaryCall[1]);
    expect(summary.risk_distribution).toEqual({
      LOW: 1,
      MEDIUM: 1,
      HIGH: 1,
    });
  });

  test('should handle writeFile errors gracefully', async () => {
    const scoredMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'fraud_detector',
      target_agent: 'reporting_agent',
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
        fraud_risk_score: 0,
        fraud_risk_level: 'LOW',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(scoredMessage));
    mockedMkdir.mockResolvedValue(undefined);
    // Fail first write (individual result) but succeed for summary
    mockedWriteFile
      .mockRejectedValueOnce(new Error('Write error'))
      .mockResolvedValue(undefined);

    await runReportingAgent();

    // Should log error but continue
    expect(console.error).toHaveBeenCalled();

    // Should still write summary
    expect(mockedWriteFile).toHaveBeenCalledWith(
      expect.stringContaining('pipeline_summary.json'),
      expect.any(String)
    );
  });

  test('should handle rejected transaction without rejection_reason field', async () => {
    const scoredMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'fraud_detector',
      target_agent: 'reporting_agent',
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
        status: 'rejected',
        fraud_risk_score: 0,
        fraud_risk_level: 'N/A',
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(scoredMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runReportingAgent();

    const summaryCall = (mockedWriteFile.mock.calls as any).find(
      (call: any) => call[0].includes('pipeline_summary.json')
    );
    const summary = JSON.parse(summaryCall[1]);
    // Should still track rejected count but not in rejection_breakdown
    expect(summary.rejected_count).toBe(1);
    expect(Object.keys(summary.rejection_breakdown).length).toBe(0);
  });
});

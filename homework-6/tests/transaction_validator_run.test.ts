import { runValidator } from '../agents/transaction_validator';
import { readFile, writeFile, readdir, mkdir } from 'node:fs/promises';
import path from 'node:path';

// Mock node:fs/promises
jest.mock('node:fs/promises');

const mockedReaddir = readdir as jest.MockedFunction<typeof readdir>;
const mockedReadFile = readFile as jest.MockedFunction<typeof readFile>;
const mockedWriteFile = writeFile as jest.MockedFunction<typeof writeFile>;
const mockedMkdir = mkdir as jest.MockedFunction<typeof mkdir>;

describe('Transaction Validator - runValidator', () => {
  const inputDir = path.join(process.cwd(), 'shared', 'input');
  const outputDir = path.join(process.cwd(), 'shared', 'output');

  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'log').mockImplementation();
    jest.spyOn(console, 'error').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('should validate transactions and write results', async () => {
    const validMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json', 'not-json.txt'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    // Verify mkdir was called
    expect(mockedMkdir).toHaveBeenCalledWith(outputDir, { recursive: true });

    // Verify readdir was called
    expect(mockedReaddir).toHaveBeenCalledWith(inputDir);

    // Verify readFile was called for JSON files only
    expect(mockedReadFile).toHaveBeenCalledTimes(1);

    // Verify writeFile was called with validated result
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.status).toBe('validated');
    expect(result.source_agent).toBe('transaction_validator');
    expect(result.target_agent).toBe('fraud_detector');
  });

  test('should reject invalid transactions', async () => {
    const invalidMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(invalidMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    // Verify writeFile was called with rejected result
    expect(mockedWriteFile).toHaveBeenCalledTimes(1);
    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.status).toBe('rejected');
    expect(result.data.rejection_reason).toBe('NEGATIVE_AMOUNT');
  });

  test('should handle empty input directory', async () => {
    mockedReaddir.mockResolvedValue([] as any);
    mockedMkdir.mockResolvedValue(undefined);

    await runValidator();

    expect(mockedReaddir).toHaveBeenCalledWith(inputDir);
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
        source_agent: 'input_loader',
        target_agent: 'transaction_validator',
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
        },
      }));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

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

    await runValidator();

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

    await expect(runValidator()).rejects.toThrow('process.exit called with 1');

    expect(console.error).toHaveBeenCalledWith(
      'Validator error:',
      'Directory not found'
    );
    expect(mockExit).toHaveBeenCalledWith(1);

    mockExit.mockRestore();
  });

  test('should process multiple transactions', async () => {
    const message1 = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    const message2 = {
      message_id: 'msg2',
      timestamp: '2026-03-22T10:01:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json', 'txn2.json'] as any);
    mockedReadFile
      .mockResolvedValueOnce(JSON.stringify(message1))
      .mockResolvedValueOnce(JSON.stringify(message2));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    expect(mockedWriteFile).toHaveBeenCalledTimes(2);

    const result1 = JSON.parse((mockedWriteFile.mock.calls as any)[0][1]);
    expect(result1.data.status).toBe('validated');

    const result2 = JSON.parse((mockedWriteFile.mock.calls as any)[1][1]);
    expect(result2.data.status).toBe('rejected');
  });

  test('should log validation decisions', async () => {
    const validMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining('TXN001 → VALIDATED')
    );
  });

  test('should handle invalid currency', async () => {
    const invalidCurrencyMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN006',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: 'ACC-2001',
        amount: '1500.00',
        currency: 'XYZ',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(invalidCurrencyMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.status).toBe('rejected');
    expect(result.data.rejection_reason).toBe('INVALID_CURRENCY');
  });

  test('should handle writeFile errors gracefully', async () => {
    const validMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
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
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(validMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockRejectedValue(new Error('Write error'));

    await runValidator();

    // Should log error but continue
    expect(console.error).toHaveBeenCalled();
  });

  test('should handle missing fields other than first', async () => {
    const missingDestinationMessage = {
      message_id: 'msg1',
      timestamp: '2026-03-22T10:00:00Z',
      source_agent: 'input_loader',
      target_agent: 'transaction_validator',
      message_type: 'transaction',
      data: {
        transaction_id: 'TXN001',
        timestamp: '2026-03-16T09:00:00Z',
        source_account: 'ACC-1001',
        destination_account: '',
        amount: '1500.00',
        currency: 'USD',
        transaction_type: 'transfer',
        description: 'Test',
        metadata: { channel: 'online', country: 'US' },
      },
    };

    mockedReaddir.mockResolvedValue(['txn1.json'] as any);
    mockedReadFile.mockResolvedValue(JSON.stringify(missingDestinationMessage));
    mockedMkdir.mockResolvedValue(undefined);
    mockedWriteFile.mockResolvedValue(undefined);

    await runValidator();

    const writeCall = (mockedWriteFile.mock.calls as any)[0];
    const result = JSON.parse(writeCall[1]);
    expect(result.data.status).toBe('rejected');
    expect(result.data.rejection_reason).toBe('MISSING_FIELD');
  });
});

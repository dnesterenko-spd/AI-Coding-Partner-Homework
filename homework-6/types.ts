import { Decimal } from 'decimal.js';

export interface TransactionData {
  transaction_id: string;
  timestamp: string;
  source_account: string;
  destination_account: string;
  amount: string; // stored as string, converted to Decimal for processing
  currency: string;
  transaction_type: string;
  description: string;
  metadata: {
    channel: string;
    country: string;
  };
}

export interface ValidationResult extends TransactionData {
  status: 'validated' | 'rejected';
  rejection_reason?: string;
}

export interface ScoredResult extends ValidationResult {
  fraud_risk_score: number;
  fraud_risk_level: 'LOW' | 'MEDIUM' | 'HIGH' | 'N/A';
}

export interface FinalResult extends ScoredResult {
  processed_at: string;
}

export interface Message<T = any> {
  message_id: string;
  timestamp: string;
  source_agent: string;
  target_agent: string;
  message_type: string;
  data: T;
}

export interface PipelineSummary {
  total_transactions: number;
  accepted_count: number;
  rejected_count: number;
  rejection_breakdown: Record<string, number>;
  risk_distribution: Record<string, number>;
  generated_at: string;
}

// Currency whitelist (ISO 4217)
export const VALID_CURRENCIES = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'CNY'];

export const maskAccount = (account: string): string => '****' + account.slice(-4);

export const getISOTimestamp = (): string => new Date().toISOString();

Run the multi-agent banking pipeline end-to-end.

## Steps

1. **Check that sample-transactions.json exists**
   Read the sample-transactions.json file to verify it contains the 8 test transactions.
   If the file is missing, show an error and stop.

2. **Clear shared/ directories**
   Remove any existing shared/ directory to ensure a clean slate.
   Use: `rm -rf shared/`
   Confirm the cleanup is complete.

3. **Run the pipeline**
   Execute: `npm run pipeline`
   This will:
   - Load transactions from sample-transactions.json into shared/input/
   - Run Transaction Validator (validates currency and amount)
   - Run Fraud Detector (scores transactions for risk)
   - Run Reporting Agent (generates results and summary)

   Show the real-time output from each agent.

4. **Show a summary of results from shared/results/**
   Read shared/results/pipeline_summary.json and display:
   - Total transactions processed
   - Number accepted vs rejected
   - Rejection breakdown by reason
   - Risk distribution (LOW, MEDIUM, HIGH, N/A)

5. **Report any transactions that were rejected and why**
   List the rejected transactions (TXN006, TXN007) with their rejection reasons:
   - TXN006: INVALID_CURRENCY (XYZ is not a valid ISO 4217 code)
   - TXN007: NEGATIVE_AMOUNT (-100.00 is invalid)

   Show this information in a clear table format.

## Expected Output

The pipeline should process 8 transactions with these results:
- 6 accepted transactions
- 2 rejected transactions
- Risk levels: 3 LOW, 2 MEDIUM, 1 HIGH, 2 N/A

## Error Handling

- If sample-transactions.json is missing, display clear error
- If npm/node is not available, provide setup instructions
- If pipeline fails partially, show which agent failed and why
# How to Run the Banking Transaction Pipeline

This guide provides step-by-step instructions for setting up and running the AI-powered multi-agent banking transaction pipeline.

## Prerequisites

Before you begin, ensure you have the following installed:

1. **Node.js** (version 20.0.0 or higher)
   ```bash
   node --version  # Should output v20.0.0 or higher
   ```

2. **npm** (comes with Node.js)
   ```bash
   npm --version  # Should output 10.0.0 or higher
   ```

3. **Git** (for cloning the repository)
   ```bash
   git --version
   ```

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd homework-6
```

### 2. Install Dependencies

Install all required Node.js packages:

```bash
npm install
```

This will install:
- TypeScript and ts-node for running TypeScript files
- decimal.js for precise monetary calculations
- uuid for message ID generation
- Jest for testing
- FastMCP for the MCP server
- All other development dependencies

### 3. Verify Installation

Check that the installation was successful:

```bash
# Check TypeScript installation
npx tsc --version

# Check Jest installation
npx jest --version

# List available npm scripts
npm run
```

## Configuration

### 1. MCP Server Configuration (Optional)

If you want to use the MCP server features with Claude Code:

1. Ensure `.mcp.json` exists in the project root with:
   ```json
   {
     "mcpServers": {
       "context7": {
         "command": "npx",
         "args": ["-y", "@upstash/context7-mcp@latest"]
       },
       "pipeline-status": {
         "command": "ts-node",
         "args": ["mcp/server.ts"]
       }
     }
   }
   ```

2. Enable MCP servers in Claude Code settings if needed

### 2. Git Hooks Configuration (Already Set)

The project includes a pre-push hook that verifies test coverage. This is already configured in `.claude/settings.local.json`.

## Running the Pipeline

### Option 1: Full Pipeline Execution

Run the complete pipeline end-to-end:

```bash
npm run pipeline
```

This will:
1. Set up the `shared/` directory structure
2. Load transactions from `sample-transactions.json`
3. Run Transaction Validator agent
4. Run Fraud Detector agent
5. Run Reporting Agent
6. Generate results in `shared/results/`

Expected output:
```
Setting up directories...
Loading transactions from sample-transactions.json...
Running Transaction Validator...
Running Fraud Detector...
Running Reporting Agent...
Pipeline completed successfully!
```

### Option 2: Run Individual Agents

You can also run agents independently:

```bash
# Run only the validator
npm run validator

# Run only the fraud detector
npm run fraud-detector

# Run only the reporter
npm run reporter
```

### Option 3: Using Claude Code Skills

If using Claude Code, you can use slash commands:

1. **Run full pipeline**:
   ```
   /run-pipeline
   ```

2. **Validate transactions only**:
   ```
   /validate-transactions
   ```

## Running Tests

### Basic Test Run

```bash
npm test
```

### Test with Coverage Report

```bash
npm run test:coverage
```

This generates a coverage report showing:
- Statement coverage (should be >99%)
- Branch coverage (should be >83%)
- Function coverage (should be >92%)
- Line coverage (should be 100%)

### Verify Coverage Threshold

```bash
npm run test:coverage:check
```

This ensures coverage meets the 80% minimum threshold. If it fails, git push will be blocked.

### View HTML Coverage Report

After running coverage tests:

```bash
# On macOS
open coverage/lcov-report/index.html

# On Linux
xdg-open coverage/lcov-report/index.html

# On Windows
start coverage/lcov-report/index.html
```

## Using the MCP Server

### Start the MCP Server

```bash
npm run mcp
```

The server provides tools for querying pipeline results:
- `get_transaction_status`: Get status of a specific transaction
- `list_pipeline_results`: Get summary of all processed transactions

### Example MCP Usage in Claude Code

1. Query a specific transaction:
   ```
   Use the get_transaction_status tool to check TXN001
   ```

2. Get pipeline summary:
   ```
   Use the list_pipeline_results tool to show all results
   ```

## Examining Results

After running the pipeline, results are available in:

### Individual Transaction Results

Check `shared/results/` for individual transaction files:

```bash
ls -la shared/results/*.json
```

Each file contains:
- `transaction_id`
- `status` (accepted/rejected)
- `rejection_reason` (if rejected)
- `fraud_risk_score` and `fraud_risk_level`
- `processed_at` timestamp

### Pipeline Summary

View the comprehensive summary:

```bash
cat shared/results/pipeline_summary.json | jq .
```

This shows:
- Total transactions processed
- Accepted/rejected counts
- Rejection reason breakdown
- Risk level distribution
- Generation timestamp

## Modifying Test Data

To test with different transactions:

1. Edit `sample-transactions.json`
2. Follow the transaction format:
   ```json
   {
     "transaction_id": "TXN009",
     "amount": "1000.00",
     "currency": "USD",
     "source_account": "ACC123456",
     "destination_account": "ACC654321",
     "country": "US"
   }
   ```
3. Run the pipeline again

## Troubleshooting

### Issue: "npm install" fails

**Solution**:
- Clear npm cache: `npm cache clean --force`
- Delete node_modules: `rm -rf node_modules package-lock.json`
- Reinstall: `npm install`

### Issue: "Cannot find module" errors

**Solution**:
- Ensure you're in the correct directory (homework-6)
- Run `npm install` again
- Check Node.js version meets requirements

### Issue: Pipeline doesn't create output files

**Solution**:
- Check that `sample-transactions.json` exists
- Verify write permissions in the project directory
- Look for error messages in the console output
- Check `shared/` subdirectories were created

### Issue: Tests fail with coverage errors

**Solution**:
- Run `npm run test:coverage` to see detailed coverage
- The 80% threshold is for CI/CD - local failures won't block commits
- Check for any failing tests before coverage

### Issue: MCP server doesn't start

**Solution**:
- Ensure all dependencies are installed: `npm install`
- Check that port 3000 is not in use
- Verify ts-node is installed: `npx ts-node --version`

## Common Commands Reference

```bash
# Main operations
npm run pipeline              # Run full pipeline
npm test                      # Run tests
npm run test:coverage         # Run tests with coverage
npm run mcp                   # Start MCP server

# Individual agents
npm run validator             # Run validator only
npm run fraud-detector        # Run fraud detector only
npm run reporter              # Run reporter only

# Utilities
npm run test:coverage:check   # Verify 80% coverage threshold
npm run prepush              # Manual pre-push hook test
```

## Next Steps

After successfully running the pipeline:

1. Review the generated results in `shared/results/`
2. Examine the test coverage report
3. Try modifying `sample-transactions.json` with edge cases
4. Explore the MCP server tools
5. Use the Claude Code skills for automation
6. Review the agent implementations in `agents/`

## Support

For issues or questions:
1. Check the error messages carefully
2. Review the logs in the console output
3. Ensure all prerequisites are met
4. Verify file permissions in the project directory

## Additional Resources

- [README.md](./README.md) - Project overview and architecture
- [specification.md](./specification.md) - Technical specification
- [agents.md](./agents.md) - Agent implementation details
- [docs/](./docs/) - Additional documentation and screenshots
# Pipeline Status MCP Server

This FastMCP server provides tools and resources to query the banking pipeline transaction processing results.

## Features

### Tools

1. **get_transaction_status**
   - Get the current status of a specific transaction by ID
   - Parameters: `transaction_id` (string)
   - Returns: Transaction details including status, fraud risk level, and processing timestamp

2. **list_pipeline_results**
   - Get a summary of all processed transactions
   - Returns: Pipeline summary with statistics and list of transactions (limited to 20)

### Resources

1. **pipeline://summary**
   - Returns the latest pipeline run summary as plain text
   - Includes total transactions, accepted/rejected counts, rejection reasons, and risk distribution

## Usage

### Start the MCP Server

```bash
npm run mcp
```

Or directly:
```bash
npx ts-node mcp/server.ts
```

### Configuration

The server is configured in `mcp.json` and can be accessed via Claude Code or any MCP-compatible client.

### Example Queries

#### Get Transaction Status
```json
{
  "tool": "get_transaction_status",
  "args": {
    "transaction_id": "TXN001"
  }
}
```

#### List All Results
```json
{
  "tool": "list_pipeline_results",
  "args": {}
}
```

#### Get Summary Resource
Access the resource: `pipeline://summary`

## Development

The server reads transaction results from `../shared/results/` directory. Make sure to run the pipeline first:

```bash
npm run pipeline
```

This will process the sample transactions and generate results that the MCP server can query.
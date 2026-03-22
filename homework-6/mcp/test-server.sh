#!/bin/bash
# Test script for the Pipeline Status MCP Server

echo "=== Testing Pipeline Status MCP Server ==="
echo ""

echo "1. Testing tools/list endpoint..."
echo '{"jsonrpc": "2.0", "method": "tools/list", "id": 1}' | npx ts-node mcp/server.ts 2>/dev/null | jq -r '.result.tools[].name'

echo ""
echo "2. Testing resources/list endpoint..."
echo '{"jsonrpc": "2.0", "method": "resources/list", "id": 2}' | npx ts-node mcp/server.ts 2>/dev/null | jq -r '.result.resources[].uri'

echo ""
echo "3. Testing get_transaction_status tool with TXN001..."
echo '{"jsonrpc": "2.0", "method": "tools/call", "params": {"name": "get_transaction_status", "arguments": {"transaction_id": "TXN001"}}, "id": 3}' | npx ts-node mcp/server.ts 2>/dev/null | jq -r '.result.content[0].text' | head -10

echo ""
echo "Test complete!"
#!/bin/bash

# Banking Transactions API - Sample Requests Script
# This script demonstrates how to use the Banking Transactions API

API_URL="http://localhost:8080"

echo "========================================="
echo "Banking Transactions API - Sample Requests"
echo "========================================="
echo ""

# Create a deposit
echo "1. Creating a DEPOSIT transaction..."
curl -X POST "$API_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "toAccount": "ACC-99999",
    "amount": 300.00,
    "currency": "USD",
    "type": "DEPOSIT"
  }' | jq .
echo ""

# Create a transfer
echo "2. Creating a TRANSFER transaction..."
curl -X POST "$API_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "ACC-12345",
    "toAccount": "ACC-67890",
    "amount": 75.50,
    "currency": "USD",
    "type": "TRANSFER"
  }' | jq .
echo ""

# Get all transactions
echo "3. Getting all transactions..."
curl -s "$API_URL/transactions" | jq .
echo ""

# Get transactions for specific account
echo "4. Getting transactions for ACC-12345..."
curl -s "$API_URL/transactions?accountId=ACC-12345" | jq .
echo ""

# Get transactions by type
echo "5. Getting all TRANSFER transactions..."
curl -s "$API_URL/transactions?type=TRANSFER" | jq .
echo ""

# Get account balance
echo "6. Getting balance for ACC-12345..."
curl -s "$API_URL/accounts/ACC-12345/balance" | jq .
echo ""

# Get account summary
echo "7. Getting summary for ACC-12345..."
curl -s "$API_URL/accounts/ACC-12345/summary" | jq .
echo ""

# Get account balance for another account
echo "8. Getting balance for ACC-67890..."
curl -s "$API_URL/accounts/ACC-67890/balance" | jq .
echo ""

# Get account summary for another account
echo "9. Getting summary for ACC-67890..."
curl -s "$API_URL/accounts/ACC-67890/summary" | jq .
echo ""

# Test validation - invalid amount
echo "10. Testing validation - negative amount (should fail)..."
curl -X POST "$API_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "ACC-12345",
    "toAccount": "ACC-67890",
    "amount": -100.00,
    "currency": "USD",
    "type": "TRANSFER"
  }' | jq .
echo ""

# Test validation - invalid currency
echo "11. Testing validation - invalid currency (should fail)..."
curl -X POST "$API_URL/transactions" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "ACC-12345",
    "toAccount": "ACC-67890",
    "amount": 100.00,
    "currency": "XXX",
    "type": "TRANSFER"
  }' | jq .
echo ""

echo "========================================="
echo "Sample requests completed!"
echo "========================================="

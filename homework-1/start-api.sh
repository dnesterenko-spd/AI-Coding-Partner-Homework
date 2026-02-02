#!/bin/bash

# Kill any existing Java processes
pkill -f "banking-api" 2>/dev/null || true
sleep 2

# Find an available port
PORT=3000
while netstat -tuln 2>/dev/null | grep -q ":$PORT "; do
    PORT=$((PORT + 1))
done

echo "Starting Banking Transactions API on port $PORT..."
cd /Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-1

# Start the API
java -jar target/banking-api-1.0.0.jar --server.port=$PORT


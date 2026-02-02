#!/bin/bash

# Banking Transactions API - Startup Script
# This script starts the Banking Transactions API on port 8080

echo "========================================="
echo "Banking Transactions API"
echo "========================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed. Please install Maven first."
    echo "Visit: https://maven.apache.org/install.html"
    exit 1
fi

echo "Building the application..."
mvn clean package -q

if [ $? -eq 0 ]; then
    echo "Build successful! Starting the API..."
    echo ""
    echo "The API will run on http://localhost:8080"
    echo ""
    mvn spring-boot:run
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi

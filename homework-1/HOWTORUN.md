# ▶️ How to Run the Banking Transactions API

## 📋 Prerequisites

Before running the application, ensure you have the following installed:

### Required
- **Java Development Kit (JDK)** 17 or higher
  - Download: https://www.oracle.com/java/technologies/downloads/
  - Verify: `java -version`

- **Maven** 3.8.0 or higher
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`

### Optional
- **Git** (for cloning/version control)
- **curl** or **Postman** (for testing API endpoints)
- **VS Code with REST Client extension** (for easy API testing)

---

## 🚀 Quick Start (3 Steps)

### Step 1: Navigate to Project Directory
```bash
cd /Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-1
```

### Step 2: Build the Project
```bash
mvn clean package
```
This command will:
- Clean previous build artifacts
- Download dependencies
- Compile source code
- Run tests (if any)
- Create JAR file in `target/` directory

### Step 3: Run the Application
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

You should see output like:
```
2026-01-30 10:30:45.123  INFO 12345 --- [           main] c.b.BankingApiApplication               : Started BankingApiApplication in 3.456 seconds (JVM running for 4.123)
```

---

## 🔧 Alternative Running Methods

### Option 1: Using the Run Script (macOS/Linux)
```bash
chmod +x demo/run.sh
./demo/run.sh
```

### Option 2: Run JAR Directly
After building with `mvn clean package`:
```bash
java -jar target/banking-api-1.0.0.jar
```

### Option 3: Run with IDE
If using IntelliJ IDEA or Eclipse:
1. Open the project
2. Right-click `BankingApiApplication.java`
3. Select "Run" or "Debug"

---

## 🧪 Testing the API

### Method 1: Using the Sample Requests Shell Script
```bash
# Make the script executable
chmod +x demo/sample-requests.sh

# Run it (requires jq for JSON formatting)
./demo/sample-requests.sh
```

### Method 2: Using HTTP Requests File
1. Install **REST Client** extension in VS Code
2. Open `demo/sample-requests.http`
3. Click **Send Request** on any request block

### Method 3: Using curl Commands

**Get all transactions:**
```bash
curl http://localhost:8080/transactions
```

**Create a new transaction:**
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "toAccount": "ACC-99999",
    "amount": 500.00,
    "currency": "USD",
    "type": "DEPOSIT"
  }'
```

**Get account balance:**
```bash
curl http://localhost:8080/accounts/ACC-12345/balance
```

**Get account summary:**
```bash
curl http://localhost:8080/accounts/ACC-12345/summary
```

**Filter transactions:**
```bash
# By account ID
curl "http://localhost:8080/transactions?accountId=ACC-12345"

# By type
curl "http://localhost:8080/transactions?type=TRANSFER"

# By date range
curl "http://localhost:8080/transactions?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59"
```

### Method 4: Using Postman
1. Download and install [Postman](https://www.postman.com/)
2. Create a new HTTP request
3. Set method to POST/GET
4. Enter URL: `http://localhost:8080/transactions`
5. For POST requests, add JSON body
6. Click **Send**

---

## 📊 Sample Data

The application comes with pre-loaded sample transactions:

**Sample Accounts:**
- `ACC-12345` - USD account with balance $249.50
- `ACC-67890` - EUR account with balance €224.75
- `ACC-11111` - GBP account with balance £1,200.00

**Sample Transactions:**
- 3 DEPOSIT transactions
- 1 WITHDRAWAL transaction
- 3 TRANSFER transactions

View sample data: `demo/sample-data.json`

---

## 🔍 Troubleshooting

### Issue: "Command not found: java"
**Solution:** 
- Install JDK 17+
- Add Java to PATH environment variable
- Verify: `java -version`

### Issue: "Command not found: mvn"
**Solution:**
- Install Maven 3.8+
- Add Maven to PATH environment variable
- Verify: `mvn -version`

### Issue: "Port 8080 is already in use"
**Solution 1:** Kill the process using port 8080
```bash
# macOS/Linux
lsof -i :8080
kill -9 <PID>
```

**Solution 2:** Change the port in `src/main/resources/application.properties`
```properties
server.port=8081
```

### Issue: Build fails with "Cannot find symbol"
**Solution:**
- Ensure Java 17+ is being used: `javac -version`
- Run: `mvn clean install`
- Check if all dependencies downloaded correctly

### Issue: "No such file or directory" for demo scripts
**Solution:**
```bash
# Make sure you're in the homework-1 directory
cd homework-1

# Make scripts executable
chmod +x demo/*.sh

# Run script
./demo/sample-requests.sh
```

---

## 📝 API Documentation

### Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/transactions` | Create transaction |
| GET | `/transactions` | Get all transactions (with filters) |
| GET | `/transactions/{id}` | Get transaction by ID |
| GET | `/accounts/{accountId}/balance` | Get account balance |
| GET | `/accounts/{accountId}/summary` | Get account summary |

### Request/Response Examples

**Create Transaction Request:**
```json
{
  "fromAccount": "ACC-12345",
  "toAccount": "ACC-67890",
  "amount": 100.50,
  "currency": "USD",
  "type": "TRANSFER"
}
```

**Create Transaction Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440099",
  "fromAccount": "ACC-12345",
  "toAccount": "ACC-67890",
  "amount": 100.50,
  "currency": "USD",
  "type": "TRANSFER",
  "timestamp": "2026-01-30T10:35:22.123456",
  "status": "COMPLETED"
}
```

**Validation Error Response (400 Bad Request):**
```json
{
  "error": "Validation failed",
  "details": [
    {
      "field": "amount",
      "message": "Amount must be a positive number"
    }
  ]
}
```

---

## 🛑 Stopping the Application

Press `Ctrl+C` in the terminal running the application.

---

## 📚 Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Maven Documentation**: https://maven.apache.org/
- **Java 17 Features**: https://www.oracle.com/java/technologies/javase/17-relnotes.html
- **REST API Best Practices**: https://restfulapi.net/

---

## ✅ Verification Checklist

After starting the application, verify:

- [ ] Application starts without errors
- [ ] Server runs on http://localhost:8080
- [ ] Can create a new transaction
- [ ] Can retrieve all transactions
- [ ] Can get account balance
- [ ] Can get account summary
- [ ] Validation works (rejects invalid requests)
- [ ] Filtering works (by account, type, date)

If all checks pass, the API is working correctly! ✨

---

<div align="center">

**Happy Testing! 🎉**

For issues or questions, check the troubleshooting section above.

</div>

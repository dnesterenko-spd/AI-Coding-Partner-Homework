# 🔌 API Reference

Complete API documentation for the Intelligent Customer Support System.

## Base URL

```
http://localhost:8080
```

## Authentication

Currently, no authentication is required. In production, implement OAuth 2.0 or JWT.

## Content Type

All requests and responses use `application/json` unless otherwise specified.

---

## 📋 Data Models

### Ticket Response

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "assignedTo": null,
  "tags": ["urgent", "login-issue"],
  "source": "web_form",
  "browser": "Chrome 120",
  "deviceType": "desktop",
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T10:30:00Z",
  "resolvedAt": null
}
```

### Create Ticket Request

```json
{
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "tags": ["urgent", "login-issue"],
  "source": "web_form",
  "browser": "Chrome 120",
  "deviceType": "desktop"
}
```

### Error Response

```json
{
  "timestamp": "2026-02-10T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/tickets",
  "validationErrors": {
    "customerEmail": "Customer email is required",
    "description": "Description must be between 10 and 2000 characters"
  }
}
```

### Bulk Import Response

```json
{
  "importBatchId": "import-20260210-001",
  "totalRecords": 100,
  "successCount": 98,
  "failureCount": 2,
  "processingTimeMs": 2340,
  "failedRecords": [
    {
      "recordNumber": 5,
      "reason": "Missing required field: customer_email"
    },
    {
      "recordNumber": 47,
      "reason": "Invalid email format: invalid@domain"
    }
  ]
}
```

---

## 🎯 Endpoints

### 1. Create Ticket

**Endpoint:** `POST /tickets`

**Description:** Create a new support ticket

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T10:30:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "customerEmail": "customer@example.com",
    "customerName": "John Doe",
    "subject": "Cannot login to account",
    "description": "I am unable to access my account despite entering correct credentials",
    "category": "ACCOUNT_ACCESS",
    "priority": "HIGH"
  }'
```

**Error Response (400 Bad Request):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validationErrors": {
    "customerEmail": "Customer email is required",
    "description": "Description must be between 10 and 2000 characters"
  }
}
```

---

### 2. Bulk Import Tickets

**Endpoint:** `POST /tickets/import`

**Description:** Import multiple tickets from CSV, JSON, or XML file

**Request Headers:**
```
Content-Type: multipart/form-data
```

**Request Parameters:**
- `file` (required) - The file to import (multipart)
- `format` (optional) - File format: CSV, JSON, or XML (auto-detected if not provided)
- `validateOnly` (optional) - Boolean flag for validation-only mode
- `importBatch` (optional) - Batch identifier for tracking

**Response (200 OK):**
```json
{
  "importBatchId": "import-20260210-001",
  "totalRecords": 50,
  "successCount": 48,
  "failureCount": 2,
  "processingTimeMs": 1250,
  "failedRecords": [
    {
      "recordNumber": 15,
      "reason": "Missing required field: subject"
    },
    {
      "recordNumber": 32,
      "reason": "Invalid email format"
    }
  ]
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@sample_tickets.csv" \
  -F "format=CSV"
```

**With validateOnly:**
```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@sample_tickets.json" \
  -F "format=JSON" \
  -F "validateOnly=true"
```

**Error Response (413 Payload Too Large):**
```json
{
  "status": 413,
  "error": "Payload Too Large",
  "message": "File size exceeds maximum allowed size of 100MB"
}
```

---

### 3. List Tickets

**Endpoint:** `GET /tickets`

**Description:** Retrieve a paginated list of tickets with optional filtering

**Request Parameters:**
- `page` (optional, default: 0) - Page number (0-indexed)
- `size` (optional, default: 20) - Records per page
- `status` (optional) - Filter by status: NEW, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED
- `category` (optional) - Filter by category: ACCOUNT_ACCESS, TECHNICAL_ISSUE, BILLING_QUESTION, FEATURE_REQUEST, BUG_REPORT, OTHER
- `priority` (optional) - Filter by priority: URGENT, HIGH, MEDIUM, LOW
- `customerId` (optional) - Filter by customer ID
- `customerEmail` (optional) - Filter by customer email
- `assignedTo` (optional) - Filter by assigned agent
- `keyword` (optional) - Search in subject and description

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "customerId": "CUST001",
      "customerEmail": "customer@example.com",
      "customerName": "John Doe",
      "subject": "Cannot login to account",
      "description": "I am unable to access my account despite entering correct credentials",
      "category": "ACCOUNT_ACCESS",
      "priority": "HIGH",
      "status": "NEW",
      "createdAt": "2026-02-10T10:30:00Z",
      "updatedAt": "2026-02-10T10:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

**cURL Examples:**
```bash
# Basic listing
curl http://localhost:8080/tickets

# With pagination
curl http://localhost:8080/tickets?page=1&size=50

# Filter by status
curl http://localhost:8080/tickets?status=NEW

# Filter by priority and category
curl http://localhost:8080/tickets?priority=HIGH&category=TECHNICAL_ISSUE

# Search by keyword
curl http://localhost:8080/tickets?keyword=login
```

---

### 4. Get Ticket by ID

**Endpoint:** `GET /tickets/{id}`

**Description:** Retrieve a specific ticket by ID

**Request Parameters:**
- `id` (required, path) - Ticket UUID

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "HIGH",
  "status": "NEW",
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T10:30:00Z"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000
```

**Error Response (404 Not Found):**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 5. Update Ticket

**Endpoint:** `PUT /tickets/{id}`

**Description:** Update an existing ticket

**Request Parameters:**
- `id` (required, path) - Ticket UUID

**Request Body:**
```json
{
  "subject": "Updated subject",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "assignedTo": "agent-001"
}
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Updated subject",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "assignedTo": "agent-001",
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T11:45:00Z"
}
```

**cURL Example:**
```bash
curl -X PUT http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "MEDIUM",
    "status": "IN_PROGRESS",
    "assignedTo": "agent-001"
  }'
```

---

### 6. Delete Ticket

**Endpoint:** `DELETE /tickets/{id}`

**Description:** Delete a ticket

**Request Parameters:**
- `id` (required, path) - Ticket UUID

**Response (204 No Content):**
```
(Empty response body)
```

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000
```

**Error Response (404 Not Found):**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 7. Auto-Classify Ticket

**Endpoint:** `POST /tickets/{id}/auto-classify`

**Description:** Trigger automatic re-classification for a ticket

**Request Parameters:**
- `id` (required, path) - Ticket UUID

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST001",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "subject": "Cannot login to account",
  "description": "I am unable to access my account despite entering correct credentials",
  "category": "ACCOUNT_ACCESS",
  "priority": "URGENT",
  "status": "NEW",
  "createdAt": "2026-02-10T10:30:00Z",
  "updatedAt": "2026-02-10T11:45:00Z",
  "classification": {
    "category": "ACCOUNT_ACCESS",
    "priority": "URGENT",
    "confidenceScore": 0.92,
    "reasoning": "Classified as 'ACCOUNT_ACCESS' based on keywords: login, password, authentication",
    "matchedKeywords": ["login", "password", "cannot access"],
    "classifiedAt": "2026-02-10T11:45:00Z",
    "isManualOverride": false
  }
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000/auto-classify
```

---

## 📊 Status Codes

| Code | Description |
|------|-------------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 204 | No Content - Successful deletion |
| 400 | Bad Request - Invalid input or validation error |
| 404 | Not Found - Resource not found |
| 413 | Payload Too Large - File exceeds size limit |
| 422 | Unprocessable Entity - Import failed with errors |
| 500 | Internal Server Error - Server error |

---

## 🏷️ Enums

### Category Values
- `ACCOUNT_ACCESS`
- `TECHNICAL_ISSUE`
- `BILLING_QUESTION`
- `FEATURE_REQUEST`
- `BUG_REPORT`
- `OTHER`

### Priority Values
- `URGENT`
- `HIGH`
- `MEDIUM`
- `LOW`

### Status Values
- `NEW`
- `IN_PROGRESS`
- `WAITING_CUSTOMER`
- `RESOLVED`
- `CLOSED`

### Device Type Values
- `desktop`
- `mobile`
- `tablet`

### Source Values
- `web_form`
- `email`
- `api`
- `chat`
- `phone`

---

## 🔗 Swagger UI

Interactive API documentation available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```
http://localhost:8080/v3/api-docs
```

---

**Last Updated**: February 2026
**API Version**: 1.0.0

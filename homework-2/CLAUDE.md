# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

This is Homework 2 for an AI-Assisted Development course. The task is to build an Intelligent Customer Support System with the following core features:
- Multi-format ticket import (CSV, JSON, XML)
- Automatic ticket categorization and priority assignment
- REST API for ticket management
- Comprehensive test suite (>85% coverage)
- Multi-level documentation for different audiences

## Tech Stack Options

The project can be implemented in one of these stacks:
- **Node.js/Express** - Use `npm` or `yarn` for package management
- **Python/Flask or FastAPI** - Use `pip` and `requirements.txt` or `poetry`
- **Java/Spring Boot** - Use Maven or Gradle

## Development Commands

### Node.js/Express Implementation
```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run specific test file
npm test -- tests/test_ticket_api.js

# Lint code
npm run lint
```

### Python/Flask or FastAPI Implementation
```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run development server (Flask)
flask run --debug

# Run development server (FastAPI)
uvicorn main:app --reload

# Run all tests
pytest

# Run tests with coverage
pytest --cov=. --cov-report=html --cov-report=term

# Run specific test file
pytest tests/test_ticket_api.py

# Run specific test
pytest tests/test_ticket_api.py::test_create_ticket
```

### Java/Spring Boot Implementation
```bash
# Build project (Maven)
mvn clean install

# Run application (Maven)
mvn spring-boot:run

# Run tests (Maven)
mvn test

# Run specific test class
mvn test -Dtest=TicketApiTest

# Generate coverage report
mvn jacoco:report
```

## Architecture Overview

The system follows a layered architecture:

1. **API Layer** (`/routes`, `/controllers`, or `/api`)
   - REST endpoints for ticket CRUD operations
   - Import endpoint handling multiple file formats
   - Auto-classification endpoint

2. **Service Layer** (`/services`)
   - `TicketService` - Core ticket business logic
   - `ImportService` - File parsing for CSV, JSON, XML
   - `ClassificationService` - Auto-categorization and priority assignment
   - `ValidationService` - Data validation logic

3. **Model/Data Layer** (`/models`)
   - Ticket model with all required fields
   - Validation schemas
   - Database configuration (if using persistence)

4. **Utilities** (`/utils`)
   - File parsers for each format
   - Keyword matchers for classification
   - Error handlers

## Key Implementation Details

### Ticket Classification Logic
- Classification runs on keywords found in subject and description
- Priority assignment based on urgency keywords
- Confidence score calculation based on keyword match strength
- Store classification metadata for audit trail

### Import Processing
- Parse files using appropriate libraries (csv-parser, xml2js for Node.js; csv, xml.etree for Python)
- Validate each record before processing
- Return detailed import summary with success/failure counts
- Handle malformed files gracefully with specific error messages

### Test Structure Requirements
The test suite must follow this structure:
```
tests/
├── test_ticket_api          # 11+ tests for API endpoints
├── test_ticket_model        # 9+ tests for validation
├── test_import_csv          # 6+ tests for CSV parsing
├── test_import_json         # 5+ tests for JSON parsing
├── test_import_xml          # 5+ tests for XML parsing
├── test_categorization      # 10+ tests for classification
├── test_integration         # 5+ tests for workflows
├── test_performance         # 5+ benchmarks
└── fixtures/                # Sample data files
```

## Documentation Requirements

Generate these 5 documentation files:

1. **README.md** - Developer-focused with Mermaid architecture diagram
2. **API_REFERENCE.md** - Complete endpoint documentation with cURL examples
3. **ARCHITECTURE.md** - Technical design with Mermaid diagrams
4. **TESTING_GUIDE.md** - QA-focused with test pyramid diagram
5. **docs/screenshots/** - Test coverage report screenshot

## Data Validation Rules

### Required Fields
- `customer_id`, `customer_email`, `customer_name`
- `subject` (1-200 chars)
- `description` (10-2000 chars)
- Valid email format
- Enum values for category, priority, status

### Auto-Classification Keywords
- **Urgent**: "can't access", "critical", "production down", "security"
- **High**: "important", "blocking", "asap"
- **Account Access**: "login", "password", "2FA", "authentication"
- **Technical Issue**: "bug", "error", "crash", "not working"
- **Billing**: "payment", "invoice", "refund", "charge"
- **Feature Request**: "enhancement", "feature", "suggestion", "would be nice"
- **Bug Report**: "defect", "reproduce", "steps to"

## Sample Data Requirements

Create these sample files in the project root:
- `sample_tickets.csv` - 50 valid tickets
- `sample_tickets.json` - 20 valid tickets
- `sample_tickets.xml` - 30 valid tickets
- `invalid_*.{csv,json,xml}` - Files with errors for negative testing

## Performance Considerations

- Bulk import should handle 1000+ records efficiently
- Use streaming for large file processing
- Implement request validation middleware
- Add rate limiting for production readiness
- Cache classification results when applicable
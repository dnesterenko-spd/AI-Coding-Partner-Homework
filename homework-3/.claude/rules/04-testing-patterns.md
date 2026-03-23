# Testing Patterns

## Unit Test Pattern (Arrange-Act-Assert)

```typescript
import { CardEncryptionService } from '../card-encryption.service';

describe('CardEncryptionService', () => {
  let service: CardEncryptionService;

  beforeEach(() => {
    service = new CardEncryptionService();
  });

  it('should encrypt and decrypt PAN correctly', () => {
    // Arrange
    const originalPAN = '4532015112830366';

    // Act
    const encrypted = service.encryptPAN(originalPAN);
    const decrypted = service.decryptPAN(encrypted);

    // Assert
    expect(decrypted).toBe(originalPAN);
    expect(encrypted).not.toContain(originalPAN); // PAN not visible in ciphertext
  });

  it('should mask PAN to last 4 digits', () => {
    // Arrange
    const pan = '4532015112830366';

    // Act
    const masked = service.maskPAN(pan);

    // Assert
    expect(masked).toBe('****0366');
  });

  it('should throw error for invalid PAN length', () => {
    // Arrange
    const invalidPAN = '123456789'; // Too short

    // Act & Assert
    expect(() => service.encryptPAN(invalidPAN)).toThrow('Invalid PAN length');
  });
});
```

---

## Integration Test Pattern

```typescript
import request from 'supertest';
import { app } from '../app';
import { resetTestDB } from './helpers/db-helper';

describe('POST /api/v1/cards', () => {
  beforeEach(async () => {
    await resetTestDB(); // Reset database before each test
  });

  it('should create card with valid data', async () => {
    // Setup
    const token = generateTestJWT({ user_id: 'test-user-123' });
    const idempotencyKey = 'test-key-' + Date.now();

    const validCardData = {
      pan: '4532015112830366', // Valid Luhn
      daily_limit: '1000.00',
      monthly_limit: '10000.00',
      expiry: '2026-12'
    };

    // Request
    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    // Assert
    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty('card_token');
    expect(response.body.last4).toBe('0366');
    expect(response.body.daily_limit).toBe('1000.00');
    expect(response.body.state).toBe('CREATED');
  });

  it('should return cached response for duplicate idempotency key', async () => {
    const token = generateTestJWT({ user_id: 'test-user-123' });
    const idempotencyKey = 'duplicate-key';

    // First request
    await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    // Second request with same key
    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', idempotencyKey)
      .send(validCardData);

    expect(response.status).toBe(201); // Returns cached response
    expect(response.body.card_token).toBeDefined(); // Same as first response
  });

  it('should return 400 for invalid PAN (Luhn check fails)', async () => {
    const invalidData = {
      pan: '4532015112830367', // Invalid Luhn
      daily_limit: '1000.00',
      monthly_limit: '10000.00'
    };

    const response = await request(app)
      .post('/api/v1/cards')
      .set('Authorization', `Bearer ${token}`)
      .set('Idempotency-Key', 'test-key')
      .send(invalidData);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('VALIDATION_ERROR');
  });
});
```

---

## Coverage Targets

- **Overall Coverage:** >85% (statements, branches, functions)
- **Services:** >90% coverage (business logic is critical)
- **Controllers:** >80% coverage
- **Utilities:** >95% coverage (pure functions are easy to test)

---

## Test Distribution

- **Unit Tests (70%):** Services, utilities, validation functions
- **Integration Tests (25%):** API endpoints with real test database
- **Security Tests (5%):** SQL injection, XSS, auth bypass, rate limit bypass

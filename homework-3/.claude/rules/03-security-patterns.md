# Security Patterns

## Card PAN Handling Pattern

```typescript
// ✅ Encrypt → Store token → Return masked

// Step 1: Encrypt PAN before storage
const pan = '4532015112830366';
const encryptedPAN = await encryptionService.encryptPAN(pan);
// encryptedPAN = "base64(IV + authTag + ciphertext)"

// Step 2: Generate unique token for external references
const cardToken = generateToken(); // UUID v4

// Step 3: Store encrypted PAN + token
await prisma.card.create({
  data: {
    encrypted_pan: encryptedPAN,
    card_token: cardToken,
    user_id: userId,
    daily_limit: '1000.00'
  }
});

// Step 4: Return masked PAN only
return {
  card_token: cardToken,
  last4: pan.slice(-4), // "0366"
  daily_limit: '1000.00',
  state: 'CREATED'
};

// NEVER return full PAN:
// ❌ return { pan: '4532015112830366' }
// ❌ return { encrypted_pan: encryptedPAN }
```

---

## Audit Logging Pattern

```typescript
// ✅ Log every state change with before/after

// Before update
const oldCard = await prisma.card.findUnique({
  where: { card_token: cardToken }
});

// Perform update
const newCard = await prisma.card.update({
  where: { card_token: cardToken },
  data: { daily_limit: '2000.00' }
});

// Create audit log
await prisma.auditLog.create({
  data: {
    user_id: userId,
    action: 'UPDATE_LIMITS',
    resource_type: 'CARD',
    resource_id: oldCard.id,
    ip_address: req.ip,
    before_state: {
      daily_limit: oldCard.daily_limit,
      monthly_limit: oldCard.monthly_limit
    },
    after_state: {
      daily_limit: newCard.daily_limit,
      monthly_limit: newCard.monthly_limit
    },
    hash: computeHash(previousHash, currentData)
  }
});
```

---

## Hash Chaining for Tamper Detection

```typescript
// Hash chaining for tamper detection
function computeHash(previousHash: string | null, recordData: object): string {
  const data = JSON.stringify(recordData);
  const hash = crypto.createHash('sha256');

  if (previousHash === null) {
    // First record: hash = SHA-256(data)
    return hash.update(data).digest('hex');
  } else {
    // Subsequent: hash = SHA-256(prev_hash + data)
    return hash.update(previousHash + data).digest('hex');
  }
}
```

---

## Idempotency Pattern

```typescript
// ✅ Check → Process → Store result atomically

async function createCard(
  userId: string,
  cardData: CreateCardDto,
  idempotencyKey: string
): Promise<CardResponse> {

  // Step 1: Check if idempotency key exists
  const existing = await prisma.idempotencyKey.findUnique({
    where: { key: idempotencyKey }
  });

  // Step 2: If exists and not expired, return cached result
  if (existing && existing.expires_at > new Date()) {
    return JSON.parse(existing.response); // Return exact same response
  }

  // Step 3: Process operation
  const encryptedPAN = await encryptionService.encryptPAN(cardData.pan);
  const cardToken = generateToken();

  // Step 4: Store result + idempotency key in transaction
  const result = await prisma.$transaction(async (tx) => {
    const card = await tx.card.create({
      data: {
        user_id: userId,
        encrypted_pan: encryptedPAN,
        card_token: cardToken,
        daily_limit: cardData.daily_limit,
        monthly_limit: cardData.monthly_limit
      }
    });

    // Audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'CREATE_CARD',
        resource_id: card.id,
        after_state: { card_token: cardToken }
      }
    });

    // Store idempotency key (24h expiry)
    const response = {
      card_token: cardToken,
      last4: cardData.pan.slice(-4)
    };

    await tx.idempotencyKey.create({
      data: {
        key: idempotencyKey,
        response: JSON.stringify(response),
        expires_at: new Date(Date.now() + 24 * 60 * 60 * 1000)
      }
    });

    return response;
  });

  return result;
}
```

---

## Key Points

1. **Encrypt all PAN data with AES-256-GCM before storage**
2. **Use tokenization (card_token) for external references**
3. **Return masked PAN only (last 4 digits) in API responses**
4. **Log all state changes with before/after snapshots**
5. **Use hash chaining (SHA-256) for audit trail integrity**
6. **Check idempotency keys for all state-changing operations**
7. **Store operation + idempotency key in single atomic transaction**

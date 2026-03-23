# Database Transaction Patterns

## Atomic Operations with Prisma

```typescript
// ✅ Use transactions for multi-step operations

async function updateCardState(
  userId: string,
  cardToken: string,
  newState: CardState
): Promise<CardResponse> {

  return await prisma.$transaction(async (tx) => {
    // Step 1: Fetch current card
    const card = await tx.card.findUnique({
      where: { card_token: cardToken }
    });

    if (!card) {
      throw new NotFoundError('Card not found');
    }

    if (card.user_id !== userId) {
      throw new ForbiddenError('Not authorized');
    }

    // Step 2: Validate state transition
    if (!isValidTransition(card.state, newState)) {
      throw new ValidationError(`Invalid transition: ${card.state} → ${newState}`);
    }

    // Step 3: Update card state
    const updatedCard = await tx.card.update({
      where: { card_token: cardToken },
      data: {
        state: newState,
        updated_at: new Date()
      }
    });

    // Step 4: Create audit log
    await tx.auditLog.create({
      data: {
        user_id: userId,
        action: 'UPDATE_STATE',
        resource_type: 'CARD',
        resource_id: card.id,
        before_state: { state: card.state },
        after_state: { state: newState }
      }
    });

    // If any step fails, entire transaction is rolled back
    // If all steps succeed, transaction is committed
    return {
      card_token: updatedCard.card_token,
      state: updatedCard.state
    };
  }, {
    isolationLevel: 'Serializable' // Highest isolation for financial data
  });
}
```

---

## Allowed State Transitions

```typescript
function isValidTransition(current: CardState, next: CardState): boolean {
  const transitions = {
    CREATED: ['ACTIVE', 'CLOSED'],
    ACTIVE: ['FROZEN', 'CLOSED'],
    FROZEN: ['ACTIVE', 'CLOSED'],
    CLOSED: [] // Terminal state - no transitions allowed
  };

  return transitions[current]?.includes(next) || false;
}
```

---

## Key Points

1. **Use `prisma.$transaction()` for multi-step operations**
2. **All operations succeed together or fail together (atomicity)**
3. **Use `Serializable` isolation level for financial operations**
4. **Validate business rules inside transaction (state transitions, limits)**
5. **ALWAYS create audit log entry within same transaction**
6. **NEVER split related operations across multiple database calls**
7. **Optimistic locking: Check version/timestamp before update**

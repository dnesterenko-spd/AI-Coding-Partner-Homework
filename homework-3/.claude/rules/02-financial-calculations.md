# Financial Calculation Patterns

## ❌ WRONG - Floating Point Arithmetic

```typescript
// WRONG - Precision loss
const dailyLimit = 0.1;
const fee = 0.2;
const total = dailyLimit + fee; // 0.30000000000004 ❌

const price = 0.9;
const quantity = 3;
const revenue = price * quantity; // 2.6999999999999997 ❌

// WRONG - Number type in database
interface Card {
  daily_limit: number; // ❌ Floating point
}
```

---

## ✅ RIGHT - Decimal.js

```typescript
// RIGHT - Exact precision
import Decimal from 'decimal.js';

// Configure banker's rounding (round half to even)
Decimal.set({ rounding: 6 }); // ROUND_HALF_EVEN

const dailyLimit = new Decimal('0.1');
const fee = new Decimal('0.2');
const total = dailyLimit.plus(fee).toFixed(2); // "0.30" ✅

const price = new Decimal('0.9');
const quantity = 3;
const revenue = price.times(quantity).toFixed(2); // "2.70" ✅

// Complex calculation chain
const spending = new Decimal('1234.56');
const cashbackRate = new Decimal('0.015'); // 1.5%
const processingFee = new Decimal('2.50');

const cashback = spending.times(cashbackRate); // 18.5184
const netCashback = cashback.minus(processingFee).toFixed(2); // "16.02" ✅

// Comparisons
const limit = new Decimal('1000.00');
const amount = new Decimal('999.99');

if (amount.lte(limit)) { // Use .lte() not <=
  console.log('Within limit');
}

// Storage - as string or NUMERIC
interface Card {
  daily_limit: string; // ✅ Store as string
  monthly_limit: string; // ✅ Store as string
}

await prisma.card.create({
  data: {
    daily_limit: new Decimal(1000).toFixed(2), // "1000.00"
    monthly_limit: '10000.00'
  }
});
```

---

## Key Points

1. **ALWAYS use `new Decimal(value)` for money, NEVER use `Number`**
2. **Store monetary values as `string` or PostgreSQL `NUMERIC` type**
3. **Use `.toFixed(2)` for exactly 2 decimal places in output**
4. **Chain operations:** `.plus()`, `.minus()`, `.times()`, `.dividedBy()`
5. **Compare with `.gte()`, `.lte()`, `.eq()` methods, not `>`, `<`, `===`**
6. **Configure banker's rounding:** `Decimal.set({ rounding: 6 })`

# Validation Patterns

## Zod Schemas with Custom Refinements

```typescript
import { z } from 'zod';
import Decimal from 'decimal.js';

// Luhn algorithm for card number validation
function luhnCheck(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '');
  let sum = 0;
  let isEven = false;

  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = parseInt(digits[i], 10);

    if (isEven) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
}

// Card creation schema
const CreateCardSchema = z.object({
  pan: z.string()
    .length(16, 'PAN must be exactly 16 digits')
    .regex(/^\d{16}$/, 'PAN must contain only digits')
    .refine(luhnCheck, 'Invalid card number (Luhn check failed)'),

  daily_limit: z.string()
    .regex(/^\d+\.\d{2}$/, 'Must have exactly 2 decimal places')
    .refine(val => {
      const amount = new Decimal(val);
      return amount.gte('0.01') && amount.lte('1000000.00');
    }, 'Daily limit must be between 0.01 and 1,000,000.00'),

  monthly_limit: z.string()
    .regex(/^\d+\.\d{2}$/, 'Must have exactly 2 decimal places')
    .refine(val => {
      const amount = new Decimal(val);
      return amount.gte('0.01') && amount.lte('10000000.00');
    }, 'Monthly limit must be between 0.01 and 10,000,000.00'),

  expiry: z.string()
    .regex(/^\d{4}-\d{2}$/, 'Expiry format must be YYYY-MM')
    .refine(val => {
      const [year, month] = val.split('-').map(Number);
      const expiryDate = new Date(year, month - 1);
      return expiryDate > new Date();
    }, 'Expiry date must be in the future')
}).refine(data => {
  // Business rule: monthly_limit >= daily_limit
  const daily = new Decimal(data.daily_limit);
  const monthly = new Decimal(data.monthly_limit);
  return monthly.gte(daily);
}, {
  message: 'Monthly limit must be >= daily limit',
  path: ['monthly_limit']
});
```

---

## Validation Middleware Factory

```typescript
function validateRequest(schema: z.ZodSchema) {
  return (req: Request, res: Response, next: NextFunction) => {
    const result = schema.safeParse(req.body);

    if (!result.success) {
      return res.status(400).json({
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Validation failed',
          details: result.error.issues
        }
      });
    }

    // Replace req.body with validated data
    req.body = result.data;
    next();
  };
}
```

---

## Usage in Routes

```typescript
app.post('/api/v1/cards',
  authMiddleware,
  validateRequest(CreateCardSchema),
  cardsController.createCard
);
```

---

## Key Points

1. **Define Zod schemas for ALL request bodies**
2. **Use custom refinements for business rules (Luhn, decimal precision, future dates)**
3. **Validate decimal precision: max 2 decimal places with regex**
4. **Use decimal.js for range validation (0.01 to 1,000,000.00)**
5. **Include business rule validation (monthly >= daily limit)**
6. **Return detailed validation errors to client**
7. **Replace req.body with validated data to ensure type safety**

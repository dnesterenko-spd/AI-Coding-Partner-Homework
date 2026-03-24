# Error Handling Patterns

## Custom Error Classes

```typescript
// Base error class
class AppError extends Error {
  constructor(
    public statusCode: number,
    public code: string,
    message: string
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

// Specific error types
class ValidationError extends AppError {
  constructor(message: string) {
    super(400, 'VALIDATION_ERROR', message);
  }
}

class NotFoundError extends AppError {
  constructor(message: string) {
    super(404, 'NOT_FOUND', message);
  }
}

class UnauthorizedError extends AppError {
  constructor(message: string) {
    super(401, 'UNAUTHORIZED', message);
  }
}

class ForbiddenError extends AppError {
  constructor(message: string) {
    super(403, 'FORBIDDEN', message);
  }
}

class RateLimitError extends AppError {
  constructor(message: string, public retryAfter: number) {
    super(429, 'RATE_LIMIT_EXCEEDED', message);
  }
}

class IdempotencyConflictError extends AppError {
  constructor(message: string) {
    super(409, 'IDEMPOTENCY_CONFLICT', message);
  }
}
```

---

## Usage in Services

```typescript
async function getCardByToken(userId: string, cardToken: string) {
  const card = await prisma.card.findUnique({
    where: { card_token: cardToken }
  });

  if (!card) {
    throw new NotFoundError('Card not found');
  }

  if (card.user_id !== userId) {
    throw new ForbiddenError('You do not own this card');
  }

  return card;
}
```

---

## Global Error Handler Middleware

```typescript
// Global error handler (must be last middleware)
function errorHandler(
  err: Error,
  req: Request,
  res: Response,
  next: NextFunction
) {
  // Log full error internally
  if (err instanceof AppError && err.statusCode >= 500) {
    logger.error(err); // Include stack trace
  } else if (err instanceof AppError) {
    logger.warn(err.message);
  } else {
    logger.error(err); // Unknown error
  }

  // Determine status code and error code
  const statusCode = err instanceof AppError ? err.statusCode : 500;
  const code = err instanceof AppError ? err.code : 'INTERNAL_ERROR';

  // Sanitize error response (NEVER include stack trace in production)
  const response: any = {
    error: {
      code,
      message: err.message
    }
  };

  // Add retry-after for rate limit errors
  if (err instanceof RateLimitError) {
    res.setHeader('Retry-After', err.retryAfter);
  }

  // NEVER include stack trace in production
  if (process.env.NODE_ENV === 'development') {
    response.error.stack = err.stack; // Only in dev
  }

  res.status(statusCode).json(response);
}

// Register middleware (at the end)
app.use(errorHandler);
```

---

## Key Points

1. **Use custom error classes with proper HTTP status codes**
2. **NEVER expose stack traces in production responses**
3. **Log full errors internally (with stack trace) for debugging**
4. **Return sanitized errors to clients: `{ error: { code, message } }`**
5. **Add retry-after headers for rate limit errors (429)**
6. **Global error handler must be registered last in middleware chain**

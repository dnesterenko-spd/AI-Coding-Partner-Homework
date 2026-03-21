# Codebase Research: API-404 — GET /api/users/:id Always Returns 404

## Bug Summary

`GET /api/users/:id` returns `{"error": "User not found"}` with HTTP 404 for every request,
even when the user ID exists in the data store. The root cause is a type mismatch: Express route
parameters are always strings, but the in-memory user records use numeric IDs. Strict equality
(`===`) is used in the lookup, so the comparison always fails.

---

## Key Findings

### 1. Route definition
**File**: `demo-bug-fix/src/routes/users.js`, line 14

```js
router.get('/api/users/:id', userController.getUserById);
```

Express captures `:id` from the URL and passes it to the controller as `req.params.id`.
`req.params` values are **always strings** — Express performs no automatic type conversion.

---

### 2. Route parameter read
**File**: `demo-bug-fix/src/controllers/userController.js`, line 19

```js
const userId = req.params.id;
```

`userId` is assigned the raw string value (e.g., `"123"`). No parsing or coercion is applied.

---

### 3. In-memory data store — numeric IDs
**File**: `demo-bug-fix/src/controllers/userController.js`, lines 8–10

```js
const users = [
  { id: 123, name: 'Alice Smith', email: 'alice@example.com' },
  { id: 456, name: 'Bob Johnson', email: 'bob@example.com' },
  { id: 789, name: 'Charlie Brown', email: 'charlie@example.com' }
];
```

All `id` values are **numbers** (`123`, `456`, `789`).

---

### 4. Buggy comparison — strict equality with mismatched types
**File**: `demo-bug-fix/src/controllers/userController.js`, line 23

```js
const user = users.find(u => u.id === userId);
```

`u.id` is a number; `userId` is a string. In JavaScript, `123 === "123"` is `false`.
Therefore `find` returns `undefined` for every call, regardless of whether the ID exists.

The bug is even acknowledged in the inline comments (lines 21–22):
```js
// BUG: req.params.id returns a string, but users array uses numeric IDs
// Strict equality (===) comparison will always fail: "123" !== 123
```

---

### 5. Always-404 response path
**File**: `demo-bug-fix/src/controllers/userController.js`, lines 25–27

```js
if (!user) {
  return res.status(404).json({ error: 'User not found' });
}
```

Because `user` is always `undefined`, this branch is always taken.

---

## Fix Direction

Parse the route parameter to a number before the comparison:

```js
const userId = parseInt(req.params.id, 10);
```

With `userId` now a number, `users.find(u => u.id === userId)` will correctly match
records in the array.

Edge cases to handle:
- Non-numeric input (e.g., `/api/users/abc`) — `parseInt` returns `NaN`; `NaN === any` is
  always `false`, so the 404 path is taken, which is acceptable behaviour.
- Negative or zero IDs — no records exist for these values; 404 is the correct response.

No other files need to change.

---

## References

- `demo-bug-fix/src/routes/users.js`
- `demo-bug-fix/src/controllers/userController.js`
- `demo-bug-fix/server.js` (reviewed; no relevant code)
- `demo-bug-fix/bugs/API-404/bug-context.md` (source of issue description)

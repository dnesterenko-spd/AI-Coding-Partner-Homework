# Implementation Plan: API-404 — Type Mismatch in getUserById

## Summary
Fix the strict-equality type mismatch that causes `GET /api/users/:id` to always return 404.
One file needs one line changed.

---

## Change 1: Parse route parameter to integer

**File**: `demo-bug-fix/src/controllers/userController.js`
**Location**: line 19, inside `getUserById`

**Before**:
```js
  const userId = req.params.id;
```

**After**:
```js
  const userId = parseInt(req.params.id, 10);
```

**Reason**: `req.params.id` is always a string. The `users` array stores numeric IDs.
Strict equality (`===`) between `"123"` and `123` is `false`.
`parseInt(req.params.id, 10)` converts the string to a number, making the comparison work.
Non-numeric input (e.g. `/api/users/abc`) returns `NaN`, which equals nothing — the existing
404 path handles that correctly.

---

## Test Command

```
cd demo-bug-fix && node --test tests/userController.test.js
```

> Note: the test file does not exist yet. The Unit Test Generator agent creates it.
> Run the implementer first (apply the code fix), then the test generator, then verify.

---

## Manual Verification (no tests needed)

Start the server and curl the endpoint:

```bash
cd demo-bug-fix && node server.js &
curl http://localhost:3000/api/users/123
# Expected: {"id":123,"name":"Alice Smith","email":"alice@example.com"}

curl http://localhost:3000/api/users/999
# Expected: {"error":"User not found"} with 404

curl http://localhost:3000/api/users/abc
# Expected: {"error":"User not found"} with 404

kill %1
```

---

## References
- `demo-bug-fix/src/controllers/userController.js`
- `context/bugs/API-404/research/verified-research.md`
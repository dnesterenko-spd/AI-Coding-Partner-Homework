# Test Report: API-404 — Type Mismatch in getUserById

Generated: 2026-03-19

---

## FIRST Compliance

### Test File: `/Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-4/demo-bug-fix/tests/userController.test.js`

| Principle       | Status | Notes |
|-----------------|--------|-------|
| Fast            | ✅     | All tests complete in < 100ms. No I/O, network, database, or filesystem operations. Tests use in-memory mock request/response objects. Total suite duration: 51ms |
| Independent     | ✅     | Each test creates its own request/response objects via `makeReq()` and `makeRes()`. No shared mutable state. No execution order dependencies. Tests can run in isolation |
| Repeatable      | ✅     | No hardcoded absolute paths, system clock dependencies, environment variables, or randomness. Deterministic in-memory user data array. Tests produce identical results on every run |
| Self-validating | ✅     | All assertions use `assert.equal()` and `assert.deepEqual()`. Clear failure messages identify broken behavior. Exit code 0 on pass, non-zero on fail. No manual inspection required |
| Timely          | ✅     | All tests target the changed code path (line 19: `parseInt(req.params.id, 10)`). No tests added for unchanged code. Written alongside the fix |

---

## Tests Generated

### Test 1: `tests/userController.test.js` — "getUserById returns the correct user when a valid numeric string ID is provided"
- **Covers**: Happy path for the parseInt fix. Verifies that a numeric string ID ("123") is correctly parsed to an integer and matched against the users array
- **Type**: Happy path

### Test 2: `tests/userController.test.js` — "getUserById finds the user when req.params.id is a numeric string (regression: type mismatch)"
- **Covers**: Regression test for the original bug. Before the fix, "456" !== 456 caused the lookup to fail. This test ensures the bug does not reoccur
- **Type**: Regression

### Test 3: `tests/userController.test.js` — "getUserById returns 404 when the parsed ID does not match any user"
- **Covers**: Edge case where the parsed integer (999) does not exist in the users array. Verifies correct 404 response
- **Type**: Edge case

### Test 4: `tests/userController.test.js` — "getUserById returns 404 when req.params.id is non-numeric"
- **Covers**: Edge case where parseInt("abc", 10) produces NaN. Verifies that NaN does not match any user ID and returns 404
- **Type**: Edge case

### Test 5: `tests/userController.test.js` — "getUserById returns 404 when req.params.id is an empty string"
- **Covers**: Edge case where parseInt("", 10) produces NaN. Verifies correct 404 handling
- **Type**: Edge case

---

## Test Run Results

**Framework Detected**: Node.js built-in test runner (`node:test` + `node:assert/strict`)

**Command**: `cd /Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-4/demo-bug-fix && node --test tests/userController.test.js`

**Exit code**: 0 (PASS)

**Output**:
```
TAP version 13
# Subtest: getUserById returns the correct user when a valid numeric string ID is provided
ok 1 - getUserById returns the correct user when a valid numeric string ID is provided
  ---
  duration_ms: 0.811708
  ...
# Subtest: getUserById finds the user when req.params.id is a numeric string (regression: type mismatch)
ok 2 - getUserById finds the user when req.params.id is a numeric string (regression: type mismatch)
  ---
  duration_ms: 0.061792
  ...
# Subtest: getUserById returns 404 when the parsed ID does not match any user
ok 3 - getUserById returns 404 when the parsed ID does not match any user
  ---
  duration_ms: 0.058542
  ...
# Subtest: getUserById returns 404 when req.params.id is non-numeric
ok 4 - getUserById returns 404 when req.params.id is non-numeric
  ---
  duration_ms: 0.052208
  ...
# Subtest: getUserById returns 404 when req.params.id is an empty string
ok 5 - getUserById returns 404 when req.params.id is an empty string
  ---
  duration_ms: 0.048875
  ...
1..5
# tests 5
# suites 0
# pass 5
# fail 0
# cancelled 0
# skipped 0
# todo 0
# duration_ms 51.125292
```

### Summary Table

| Test Name | Result |
|-----------|--------|
| getUserById returns the correct user when a valid numeric string ID is provided | ✅ PASS |
| getUserById finds the user when req.params.id is a numeric string (regression: type mismatch) | ✅ PASS |
| getUserById returns 404 when the parsed ID does not match any user | ✅ PASS |
| getUserById returns 404 when req.params.id is non-numeric | ✅ PASS |
| getUserById returns 404 when req.params.id is an empty string | ✅ PASS |

---

## Coverage of Changed Code

### Changed Function: `getUserById` (line 19)

**Change**: `const userId = req.params.id;` → `const userId = parseInt(req.params.id, 10);`

- [x] **Covered** — Happy path: numeric string parsed to integer and matched (Test 1, Test 2)
- [x] **Covered** — Regression: string-to-number type mismatch bug does not reoccur (Test 2)
- [x] **Covered** — Edge case: parsed integer not found in users array (Test 3)
- [x] **Covered** — Edge case: non-numeric input produces NaN (Test 4)
- [x] **Covered** — Edge case: empty string produces NaN (Test 5)

### Unchanged Functions

- `getAllUsers` — Not covered (no changes made to this function)

---

## Conclusion

All 5 tests PASS and fully comply with FIRST principles. The changed code path (line 19: `parseInt(req.params.id, 10)`) is comprehensively covered with happy path, regression, and edge case tests. No untested code paths remain in the modified function.

**Test Framework**: Node.js built-in test runner
**Test File**: `/Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-4/demo-bug-fix/tests/userController.test.js`
**Total Tests**: 5
**Passed**: 5
**Failed**: 0
**Duration**: 51ms

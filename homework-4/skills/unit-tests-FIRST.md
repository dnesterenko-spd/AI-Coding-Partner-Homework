---
name: unit-tests-FIRST
description: Use this skill to verify that unit tests satisfy FIRST principles before
  submitting them. Defines checklists for Fast, Independent, Repeatable,
  Self-validating, and Timely — with a compliance table format agents must complete.
---

# Skill: Unit Tests — FIRST Principles

Use this skill to verify that every generated unit test satisfies the FIRST principles before it is submitted. Work through the checklist for each principle; a test may not be submitted if any **required** item is unchecked.

---

## Principles and Checklists

### F — Fast
Tests run in milliseconds. No I/O, no network, no database, no filesystem.

- [ ] Test completes in < 100 ms on a standard developer machine
- [ ] No `fs.*`, `http.*`, `fetch`, database queries, or `setTimeout` > 0 inside the test body
- [ ] External dependencies are replaced with in-memory stubs or fakes
- [ ] No `sleep` / `delay` calls

### I — Independent
Each test sets up its own state. Tests do not share mutable state or depend on execution order.

- [ ] Test has its own `beforeEach` / setup if shared fixtures exist — nothing is left over from another test
- [ ] No module-level mutable variables mutated across tests without reset
- [ ] Test passes when run in isolation (`--testNamePattern` / single-test run)
- [ ] Test passes when run in any order relative to other tests

### R — Repeatable
Same result in every environment: local machine, CI, any OS, any run order.

- [ ] No hardcoded absolute paths
- [ ] No dependency on system clock without injection or mocking
- [ ] No dependency on environment variables that may be absent in CI
- [ ] No randomness that is not seeded deterministically
- [ ] Test produces the same outcome on 10 consecutive runs

### S — Self-Validating
The test reports pass or fail automatically. No manual inspection of output required.

- [ ] Every assertion uses `assert.*` / `expect(…).to*` — no bare `console.log` inspections
- [ ] Failure message is descriptive enough to identify the broken behaviour without reading the source
- [ ] Test returns a definitive pass/fail exit code when run via the project test command
- [ ] No `// TODO: verify manually` comments

### T — Timely
Tests are written for the code that was changed, not retrofitted to unrelated code.

- [ ] Every test targets a function, branch, or behaviour that was introduced or modified in this change
- [ ] No tests added for code paths that were not touched by the change
- [ ] Tests are added before or alongside the fix, not after unrelated refactors

---

## How to Apply This Skill

1. After generating each test, run through all five checklists above.
2. If any **required** item is unchecked, revise the test until it passes.
3. In your output, include a FIRST compliance table:

```
| Principle | Status | Notes |
|-----------|--------|-------|
| Fast        | ✅ / ❌ | … |
| Independent | ✅ / ❌ | … |
| Repeatable  | ✅ / ❌ | … |
| Self-validating | ✅ / ❌ | … |
| Timely      | ✅ / ❌ | … |
```

4. Only submit tests where all five principles are ✅.

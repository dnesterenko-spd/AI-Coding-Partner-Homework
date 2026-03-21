# Homework 4: 4-Agent Pipeline

A 4-agent agentic pipeline that finds, verifies, fixes, secures, and tests a bug in a Node.js/Express API — end to end, without human intervention between stages.

---

## The Bug

`GET /api/users/:id` always returned HTTP 404, even for valid IDs.

**Root cause**: `req.params.id` is a string (`"123"`), but the in-memory user records store numeric IDs (`123`). Strict equality (`===`) between the two types always fails.

**Fix**: `parseInt(req.params.id, 10)` in `userController.js:19`.

---

## Pipeline

```
Bug Researcher ──► Research Verifier ──► Bug Planner ──► Bug Implementer
                                                               │
                                              ┌────────────────┴────────────────┐
                                              ▼                                 ▼
                                     Security Verifier               Unit Test Generator
```

| Stage | Agent | Input | Output |
|---|---|---|---|
| 1 | *(manual)* Bug Researcher | codebase | `research/codebase-research.md` |
| 2 | Research Verifier | `codebase-research.md` | `research/verified-research.md` |
| 3 | *(manual)* Bug Planner | verified research | `implementation-plan.md` |
| 4 | Bug Implementer | `implementation-plan.md` | `fix-summary.md` + patched code |
| 5a | Security Verifier | `fix-summary.md` + changed files | `security-report.md` |
| 5b | Unit Test Generator | `fix-summary.md` + changed files | test files + `test-report.md` |

---

## Project Structure

```
homework-4/
├── README.md
├── HOWTORUN.md
├── STUDENT.md
├── agents/
│   ├── research-verifier.agent.md
│   ├── bug-implementer.agent.md
│   ├── security-verifier.agent.md
│   └── unit-test-generator.agent.md
├── skills/
│   ├── research-quality-measurement.md
│   └── unit-tests-FIRST.md
├── context/bugs/API-404/
│   ├── research/
│   │   ├── codebase-research.md
│   │   └── verified-research.md
│   ├── implementation-plan.md
│   ├── fix-summary.md
│   ├── security-report.md
│   └── test-report.md
├── demo-bug-fix/          ← the app (with bug fixed)
│   ├── server.js
│   ├── src/
│   │   ├── controllers/userController.js
│   │   └── routes/users.js
│   └── tests/
│       └── userController.test.js
└── docs/screenshots/
```

---

## Skills

- **`skills/research-quality-measurement.md`** — 5-level quality rubric (EXCELLENT → UNACCEPTABLE) with a 4-dimension weighted scoring formula. Used by the Research Verifier.
- **`skills/unit-tests-FIRST.md`** — FIRST principle checklists (Fast, Independent, Repeatable, Self-validating, Timely). Used by the Unit Test Generator.

---

## Running the App

```bash
cd demo-bug-fix
npm install
npm start
# server runs on http://localhost:3000

curl http://localhost:3000/api/users        # list all users
curl http://localhost:3000/api/users/123    # get user by ID (was broken, now fixed)
curl http://localhost:3000/health           # health check
```

## Running the Tests

```bash
cd demo-bug-fix
npm test
```
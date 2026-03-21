# How to Run

## Prerequisites

- Node.js 18 or later
- Claude Code CLI (`claude`) — to run the agents interactively

---

## 1. Install app dependencies

```bash
cd demo-bug-fix
npm install
```

---

## 2. Run the app

```bash
npm start
# → Demo API server running on http://localhost:3000
```

Verify the fix works:

```bash
curl http://localhost:3000/api/users/123
# → {"id":123,"name":"Alice Smith","email":"alice@example.com"}

curl http://localhost:3000/api/users
# → [{"id":123,...}, {"id":456,...}, {"id":789,...}]
```

---

## 3. Run the unit tests

```bash
cd demo-bug-fix
npm test
# → node --test tests/userController.test.js
```

---

## 4. Run the pipeline agents

Each agent is a Markdown prompt file. Load it into Claude Code to run it.

### Step 1 — Research Verifier
```bash
# from homework-4/
claude agents/research-verifier.agent.md
```
Reads `context/bugs/API-404/research/codebase-research.md`
Writes `context/bugs/API-404/research/verified-research.md`

### Step 2 — Bug Implementer
```bash
claude agents/bug-implementer.agent.md
```
Reads `context/bugs/API-404/implementation-plan.md`
Writes `context/bugs/API-404/fix-summary.md` and patches the source

### Step 3a — Security Verifier
```bash
claude agents/security-verifier.agent.md
```
Reads `context/bugs/API-404/fix-summary.md`
Writes `context/bugs/API-404/security-report.md`

### Step 3b — Unit Test Generator
```bash
claude agents/unit-test-generator.agent.md
```
Reads `context/bugs/API-404/fix-summary.md`
Writes `demo-bug-fix/tests/userController.test.js` and `context/bugs/API-404/test-report.md`

---

## 5. Review pipeline outputs

All outputs land in `context/bugs/API-404/`:

| File | Produced by |
|------|-------------|
| `research/codebase-research.md` | Bug Researcher (manual) |
| `research/verified-research.md` | Research Verifier agent |
| `implementation-plan.md` | Bug Planner (manual) |
| `fix-summary.md` | Bug Implementer agent |
| `security-report.md` | Security Verifier agent |
| `test-report.md` | Unit Test Generator agent |
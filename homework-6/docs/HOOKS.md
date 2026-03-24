# Git Hooks Documentation

## Coverage Gate Hook

This project enforces **80% minimum test coverage** before allowing git pushes.

### How it Works

1. **Pre-push Hook**: Runs automatically before `git push`
2. **Coverage Check**: Executes `npm test:coverage`
3. **Enforcement**: Blocks push if coverage < 80% on any metric (statements, branches, functions, lines)

### Current Coverage Requirements

| Metric | Required | Current |
|--------|----------|---------|
| Statements | 80% | 41.02% ❌ |
| Branches | 80% | 38.46% ❌ |
| Functions | 80% | 30.76% ❌ |
| Lines | 80% | 41.49% ❌ |

### Installation

The hook is automatically installed, but if you need to reinstall:

```bash
./scripts/install-hooks.sh
```

### Manual Hook Test

To test if the hook is working:

```bash
bash .git/hooks/pre-push
```

### Bypass (Emergency Only)

If you absolutely must push without meeting coverage requirements:

```bash
git push --no-verify
```

⚠️ **Warning**: Bypassing coverage requirements is strongly discouraged and should only be used in emergencies.

### Improving Coverage

To meet the 80% requirement, add tests for:

1. **agents/reporting_agent.ts** - Currently at 15% coverage
2. **agents/transaction_validator.ts** - Currently at 42% coverage
3. **agents/fraud_detector.ts** - Currently at 59% coverage

Run coverage report to see uncovered lines:

```bash
npm run test:coverage
```

### Claude Code Hook

Additionally, there's a Claude Code hook in `.claude/settings.local.json` that provides the same protection when pushing through Claude Code.

## Hook Implementation Details

**Location**: `.git/hooks/pre-push`

**What it does**:
1. Changes to homework-6 directory
2. Runs `npm test:coverage`
3. Checks exit code (0 = pass, 1 = fail)
4. Blocks push on failure with helpful message

**Why it's important**:
- Maintains code quality
- Ensures tested code in production
- Prevents regression bugs
- Enforces team standards
#!/bin/bash

# Script to install git hooks for the project
# This ensures coverage requirements are enforced

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
HOMEWORK6_DIR="$(dirname "$SCRIPT_DIR")"
REPO_ROOT="$(dirname "$HOMEWORK6_DIR")"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

echo "📦 Installing git hooks for homework-6..."
echo "Repository root: $REPO_ROOT"
echo "Hooks directory: $HOOKS_DIR"

# Check if .git/hooks exists
if [ ! -d "$HOOKS_DIR" ]; then
    echo "❌ Error: $HOOKS_DIR does not exist"
    echo "Make sure you're in a git repository"
    exit 1
fi

# Create pre-push hook
cat > "$HOOKS_DIR/pre-push" << 'EOF'
#!/bin/bash

# Git pre-push hook to enforce 80% test coverage for homework-6
# This hook runs before git push and blocks if coverage is below 80%

echo "🧪 Running test coverage check before push..."

# Get the repository root
REPO_ROOT="$(git rev-parse --show-toplevel)"
HOMEWORK6_DIR="$REPO_ROOT/homework-6"

# Check if homework-6 directory exists
if [ ! -d "$HOMEWORK6_DIR" ]; then
    echo "✅ No homework-6 directory found, skipping coverage check"
    exit 0
fi

# Change to the homework-6 directory
cd "$HOMEWORK6_DIR" || exit 1

# Check if package.json exists (Node.js project)
if [ ! -f "package.json" ]; then
    echo "✅ No package.json in homework-6, skipping coverage check"
    exit 0
fi

# Run the coverage test
if npm run test:coverage > /dev/null 2>&1; then
    echo "✅ Coverage check passed! (≥80%)"
    exit 0
else
    echo "❌ Coverage check failed!"
    echo "Test coverage must be at least 80% to push."
    echo ""
    echo "Current coverage is below the required threshold."
    echo "Run 'cd homework-6 && npm test:coverage' to see detailed coverage report."
    echo ""
    echo "To bypass this check (not recommended), use: git push --no-verify"
    exit 1
fi
EOF

# Make the hook executable
chmod +x "$HOOKS_DIR/pre-push"

echo "✅ Git hooks installed successfully!"
echo ""
echo "The pre-push hook will now enforce 80% test coverage for homework-6."
echo "Run 'npm test:coverage' to check your current coverage."
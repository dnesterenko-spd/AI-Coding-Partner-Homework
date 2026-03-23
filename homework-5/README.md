# Homework 5: MCP Servers Configuration

**Student:** Dmytro Nesterenko
**Course:** AI Coding Partner
**Date:** March 2026

## Overview

This homework demonstrates the configuration and usage of three external MCP servers (GitHub, Filesystem, Jira) and one custom MCP server built with FastMCP.

## Task 1: GitHub MCP Server Configuration

### Setup Instructions

1. **Generate a GitHub Personal Access Token**:
   - Go to GitHub Settings → Developer settings → Personal access tokens
   - Click "Generate new token" (classic or fine-grained)
   - Select the following scopes (for classic token):
     - `repo` (Full control of private repositories)
     - `read:org` (Read org and team membership, read org projects)
     - `read:user` (Read ALL user profile data)
   - Copy the generated token

2. **Configure the Token** (choose one method):

   **Option A - Environment Variable (Recommended)**:
   ```bash
   # Add to your shell profile (~/.zshrc or ~/.bashrc)
   export GITHUB_MCP_TOKEN="your-actual-github-token"
   source ~/.zshrc  # or source ~/.bashrc
   ```

   **Option B - Direct in .mcp.json**:
   - Open `.mcp.json` file
   - Replace `GITHUB_MCP_TOKEN` with your actual GitHub token
   - Save the file
   - Note: Be careful not to commit this file with your token

3. **Verify Installation**:
   - The GitHub MCP server will be automatically installed via npx when Claude Code starts
   - The server uses the `@modelcontextprotocol/server-github` package

### Available Operations

The GitHub MCP server provides the following capabilities:
- List repositories
- Get repository details
- List pull requests
- Get pull request details
- List issues
- Get issue details
- List commits
- Get commit details
- Create issues
- Create pull requests
- Add comments to issues/PRs

### Usage Examples

Once configured, you can ask Claude to:
- "List my recent pull requests"
- "Show me the open issues in repository X"
- "Summarize the recent commits in my project"
- "Create a new issue with title Y"

## Project Structure

```
homework-5/
├── README.md (this file)
├── TASKS.md (assignment requirements)
├── .mcp.json (MCP server configuration)
├── HOWTORUN.md (detailed run instructions)
├── custom-mcp-server/
│   ├── server.py (FastMCP server implementation)
│   ├── lorem-ipsum.md (source text file)
│   └── requirements.txt (Python dependencies)
└── docs/
    └── screenshots/
        ├── github-mcp-result.png (to be added)
        ├── filesystem-mcp-result.png (to be added)
        ├── jira-mcp-result.png (to be added)
        └── custom-mcp-result.png (to be added)
```

## Task 4: Custom MCP Server with FastMCP

### Overview

Built a custom MCP server using FastMCP that provides:
- **Resource**: `lorem://text` - A URI that Claude can read from
- **Tool**: `read` - An action Claude can call to retrieve text

### Implementation

The custom server is located in `custom-mcp-server/` and includes:

1. **server.py**: FastMCP server implementation with:
   - `@mcp.resource("lorem://text")` decorator for the resource
   - `@mcp.tool()` decorator for the read tool
   - Both accept optional `word_count` parameter (default: 30)

2. **lorem-ipsum.md**: Source text file containing Lorem Ipsum content

3. **requirements.txt**: Python dependencies (fastmcp)

### Key Concepts

- **Resources**: Passive URIs that provide data (like files or API endpoints)
  - Example: `lorem://text` returns text from lorem-ipsum.md
  - Claude can read from resources but doesn't "call" them

- **Tools**: Active functions that Claude can invoke
  - Example: `read(word_count=50)` is a function call
  - Claude explicitly calls tools to perform operations

### Configuration

Added to `.mcp.json`:
```json
"lorem-ipsum": {
  "command": "python3",
  "args": [
    "/Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-5/custom-mcp-server/server.py"
  ]
}
```

### Usage

After restarting Claude Code with `/mcp`:
- "Use the read tool to get 50 words from lorem ipsum"
- "Call the read tool with word_count=20"
- "Read the lorem://text resource"

## Tasks Progress

- [x] Task 1: GitHub MCP Server - Configuration created
- [x] Task 2: Filesystem MCP Server - Configuration created
- [ ] Task 3: Jira MCP Server - Needs community package (see HOWTORUN.md)
- [x] Task 4: Custom MCP Server with FastMCP - Complete

## Notes

- The `.mcp.json` file is used by Claude Code to configure MCP servers
- Each server runs as a separate process and communicates via the Model Context Protocol
- Make sure to keep your GitHub token secure and never commit it to version control
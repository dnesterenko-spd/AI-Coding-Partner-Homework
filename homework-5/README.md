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
├── HOWTORUN.md (to be created - detailed run instructions)
├── custom-mcp-server/ (to be created for Task 4)
│   ├── server.py
│   ├── lorem-ipsum.md
│   └── requirements.txt
└── docs/
    └── screenshots/
        ├── github-mcp-result.png (to be added)
        ├── filesystem-mcp-result.png (to be added)
        ├── jira-mcp-result.png (to be added)
        └── custom-mcp-result.png (to be added)
```

## Tasks Progress

- [x] Task 1: GitHub MCP Server - Configuration created
- [ ] Task 2: Filesystem MCP Server
- [ ] Task 3: Jira MCP Server
- [ ] Task 4: Custom MCP Server with FastMCP

## Notes

- The `.mcp.json` file is used by Claude Code to configure MCP servers
- Each server runs as a separate process and communicates via the Model Context Protocol
- Make sure to keep your GitHub token secure and never commit it to version control
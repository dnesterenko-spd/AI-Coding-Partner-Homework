# How to Run and Configure MCP Servers

## Prerequisites

- Node.js and npm installed (version 14.x or higher)
- Claude Code (or Claude Desktop) installed
- GitHub account (for GitHub MCP)
- Jira account with API access (for Jira MCP)
- Python 3.8+ (for custom MCP server)

## Task 1: GitHub MCP Server

### Installation and Configuration

1. **Generate GitHub Personal Access Token**:
   ```bash
   # Navigate to: https://github.com/settings/tokens
   # Click "Generate new token (classic)"
   # Select scopes: repo, read:org, read:user
   # Generate and copy the token
   ```

2. **Configure the MCP Server**:
   ```bash
   # Edit .mcp.json file
   # Replace YOUR_GITHUB_TOKEN_HERE with your actual token
   ```

3. **Restart Claude Code**:
   - Close and reopen Claude Code
   - The GitHub MCP server will be automatically installed via npx

4. **Verify Connection**:
   - In Claude Code, type: "Show me my GitHub repositories"
   - You should see a list of your repositories

### Testing GitHub MCP

Try these commands in Claude Code:
```
- "List my recent pull requests"
- "Show me open issues in [repository-name]"
- "Summarize recent commits in [repository-name]"
- "Create an issue titled 'Test Issue' in [repository-name]"
```

## Task 2: Filesystem MCP Server (To be configured)

### Installation and Configuration

1. **Add to .mcp.json**:
   ```json
   "filesystem": {
     "command": "npx",
     "args": [
       "-y",
       "@modelcontextprotocol/server-filesystem@latest",
       "/path/to/your/directory"
     ]
   }
   ```

2. **Restart Claude Code**

3. **Test with**:
   - "List files in the configured directory"
   - "Read the content of [filename]"

## Task 3: Jira MCP Server

### ⚠️ Important Note

The package `@modelcontextprotocol/server-atlassian` **does not exist** in the npm registry. You need to use a community-built Jira MCP server instead.

### Recommended Alternatives

1. **`@rui.branco/jira-mcp`** (v1.6.12) - Specifically designed for Claude Code
2. **`@guhcostan/jira-mcp`** (v2.5.0) - Comprehensive with 36 tools
3. **`jira-mcp`** by camdenclark - Simple and reliable

### Installation and Configuration

1. **Generate Jira API Token**:
   - Go to: https://id.atlassian.com/manage-profile/security/api-tokens
   - Click "Create API token"
   - Give it a name and copy the token

2. **Set Environment Variables**:
   ```bash
   export ATLASSIAN_URL="https://your-domain.atlassian.net"
   export ATLASSIAN_EMAIL="your-email@example.com"
   export ATLASSIAN_API_TOKEN="your-api-token-here"
   ```

   Add to your shell profile for persistence:
   ```bash
   echo 'export ATLASSIAN_URL="https://your-domain.atlassian.net"' >> ~/.zshrc
   echo 'export ATLASSIAN_EMAIL="your-email@example.com"' >> ~/.zshrc
   echo 'export ATLASSIAN_API_TOKEN="your-api-token"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. **Update .mcp.json** with a working package (example using @rui.branco/jira-mcp):
   ```json
   "jira": {
     "command": "npx",
     "args": [
       "-y",
       "@rui.branco/jira-mcp@latest"
     ],
     "env": {
       "ATLASSIAN_URL": "${ATLASSIAN_URL}",
       "ATLASSIAN_EMAIL": "${ATLASSIAN_EMAIL}",
       "ATLASSIAN_API_TOKEN": "${ATLASSIAN_API_TOKEN}"
     }
   }
   ```

4. **Restart Claude Code**:
   ```
   /mcp
   ```

### Testing Jira MCP

Try these commands:
```
- "Give me the Jira tickets of the last 5 bugs on project XYZ"
- "Show me all open issues assigned to me"
- "List issues in the current sprint"
- "Get details for ticket ABC-123"
```

### Environment Variables Reference

The Jira MCP server requires these three variables:
- **ATLASSIAN_URL**: Base URL of your Atlassian instance (e.g., `https://yourcompany.atlassian.net`)
- **ATLASSIAN_EMAIL**: Your email used to log into Jira
- **ATLASSIAN_API_TOKEN**: API token generated from Atlassian security settings

## Task 4: Custom MCP Server with FastMCP ✅

### What You'll Build

A custom MCP server that provides:
- **Resource**: `lorem://text` - A URI Claude can read from
- **Tool**: `read` - An action Claude can call with optional `word_count` parameter

**Key Concepts**:
- **Resources** are passive data sources (like files or APIs) that Claude can read
- **Tools** are active functions that Claude can invoke to perform operations

### Installation

1. **Install Python Dependencies**:
   ```bash
   cd custom-mcp-server
   pip3 install -r requirements.txt
   ```

   Or install fastmcp directly:
   ```bash
   pip3 install fastmcp
   ```

2. **Verify Installation**:
   ```bash
   python3 -c "import fastmcp; print('FastMCP installed successfully!')"
   ```

### Configuration

The server is already configured in `.mcp.json`:
```json
"lorem-ipsum": {
  "command": "python3",
  "args": [
    "/Users/dima/Work/Projects/AI-Coding-Partner-Homework/homework-5/custom-mcp-server/server.py"
  ]
}
```

### How It Works

The `server.py` file implements:

1. **Resource** (`lorem://text`):
   - Reads from `lorem-ipsum.md`
   - Accepts `word_count` parameter (default: 30)
   - Returns exactly that many words

2. **Tool** (`read`):
   - Takes optional `word_count` parameter
   - Returns word-limited content from the resource
   - Can be invoked directly by Claude

### Testing Manually

Test the server directly (it will wait for MCP protocol messages):
```bash
python3 custom-mcp-server/server.py
```

Press Ctrl+C to stop.

### Usage in Claude Code

After restarting Claude Code with `/mcp`, test with:

1. **Test the read tool**:
   ```
   - "Use the read tool to get 50 words from lorem ipsum"
   - "Call the read tool with word_count=20"
   - "Use the lorem-ipsum server's read tool with 100 words"
   ```

2. **Test the resource**:
   ```
   - "Read the lorem://text resource"
   - "Read lorem://text with 40 words"
   ```

### Verification Checklist

- ✅ `fastmcp` is listed in `requirements.txt`
- ✅ `server.py` implements both Resource and Tool
- ✅ `lorem-ipsum.md` contains source text (180+ words)
- ✅ `.mcp.json` has correct absolute path to `server.py`
- ✅ Server runs without errors: `python3 custom-mcp-server/server.py`
- ✅ MCP client detects the server after restart

### File Structure

```
custom-mcp-server/
├── server.py           # FastMCP server implementation
├── lorem-ipsum.md      # Source text file
└── requirements.txt    # Python dependencies (fastmcp)
```

## Troubleshooting

### Common Issues

1. **MCP Server Not Found**:
   - Ensure npm/npx is in your PATH
   - Try running the npx command manually to check for errors

2. **Authentication Failed**:
   - Verify your tokens are correct
   - Check token permissions/scopes
   - Ensure tokens haven't expired

3. **Connection Refused**:
   - Check if the server is running
   - Verify the port is not blocked
   - Check firewall settings

4. **Claude Code Not Detecting MCP Servers**:
   - Ensure .mcp.json is in the correct location
   - Validate JSON syntax
   - Restart Claude Code after configuration changes

### Logs and Debugging

- Check Claude Code logs: `~/.claude/logs/`
- Run servers manually to see error output:
  ```bash
  npx @modelcontextprotocol/server-github@latest
  ```

## Security Notes

- **Never commit tokens to version control**
- Add `.mcp.json` to `.gitignore` if it contains sensitive data
- Use environment variables for tokens in production
- Rotate tokens regularly
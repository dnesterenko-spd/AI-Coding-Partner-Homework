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

## Task 3: Jira MCP Server (To be configured)

### Installation and Configuration

1. **Generate Jira API Token**:
   - Go to: https://id.atlassian.com/manage-profile/security/api-tokens
   - Create new API token

2. **Add to .mcp.json**:
   ```json
   "jira": {
     "command": "npx",
     "args": [
       "-y",
       "@modelcontextprotocol/server-jira@latest"
     ],
     "env": {
       "JIRA_URL": "https://your-domain.atlassian.net",
       "JIRA_EMAIL": "your-email@example.com",
       "JIRA_API_TOKEN": "your-api-token"
     }
   }
   ```

3. **Test with**:
   - "Give me the Jira tickets of the last 5 bugs on [project-name]"

## Task 4: Custom MCP Server (To be implemented)

### Installation

1. **Install Dependencies**:
   ```bash
   cd custom-mcp-server
   pip install -r requirements.txt
   ```

2. **Configure in .mcp.json**:
   ```json
   "custom-lorem": {
     "command": "python",
     "args": ["custom-mcp-server/server.py"]
   }
   ```

3. **Run the Server**:
   ```bash
   python custom-mcp-server/server.py
   ```

4. **Test the Tool**:
   - "Use the read tool to get 50 words from lorem ipsum"
   - "Read 10 words from the lorem resource"

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
#!/usr/bin/env python3
"""
Custom MCP Server using FastMCP
Provides a resource and tool for reading lorem-ipsum.md with word count control
"""

from pathlib import Path
from fastmcp import FastMCP

# Initialize FastMCP server
mcp = FastMCP("lorem-ipsum-server")

# Path to lorem-ipsum.md
LOREM_FILE = Path(__file__).parent / "lorem-ipsum.md"


def read_lorem_words(word_count: int = 30) -> str:
    """Read specified number of words from lorem-ipsum.md"""
    if not LOREM_FILE.exists():
        return "Error: lorem-ipsum.md not found"

    content = LOREM_FILE.read_text()
    words = content.split()

    if word_count <= 0:
        return "Error: word_count must be positive"

    # Return the requested number of words
    selected_words = words[:word_count]
    return " ".join(selected_words)


@mcp.resource("lorem://text")
def get_lorem_resource() -> str:
    """
    Resource URI that returns lorem ipsum text (default 30 words)

    Returns:
        Lorem ipsum text limited to 30 words
    """
    return read_lorem_words(30)


@mcp.tool()
def read(word_count: int = 30) -> str:
    """
    Read lorem ipsum text with specified word count

    This tool allows Claude to retrieve a specified number of words
    from the lorem-ipsum.md file.

    Args:
        word_count: Number of words to return (default: 30)

    Returns:
        Lorem ipsum text limited to word_count words
    """
    return read_lorem_words(word_count)


if __name__ == "__main__":
    # Run the MCP server
    mcp.run()

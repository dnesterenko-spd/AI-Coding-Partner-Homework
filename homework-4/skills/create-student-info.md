---
name: create-student-info
description: Use this skill to create a STUDENT.md file in the current homework folder. Analyzes TASKS.md to extract homework number and name, then generates a properly formatted student information file.
---

# Skill: Create Student Information File

This skill creates a STUDENT.md file in the current directory by analyzing the TASKS.md file to extract homework information.

## Instructions

1. **Check for Student Name in Memory**:
   - Look for a memory file containing the student's name (e.g., `user_name.md` or `student_name.md`)
   - Read MEMORY.md index to find if a user name memory exists
   - If not found, use AskUserQuestion to ask for the student's full name
   - Save the name to memory using this format:
     ```markdown
     ---
     name: Student Name
     description: User's full name for academic assignments
     type: user
     ---

     {Student's Full Name}
     ```
   - Update MEMORY.md to include a link to this memory file

2. **Locate TASKS.md**: Check if TASKS.md exists in the current working directory

3. **Extract Homework Information**:
   - Read the first line of TASKS.md
   - Extract homework number (e.g., "Homework 2", "Homework 3", etc.)
   - Extract homework title/description (text after the colon)
   - Example: `# 🎧 Homework 2: Intelligent Customer Support System`
     - Number: 2
     - Title: Intelligent Customer Support System

4. **Determine Folder Context**:
   - Use current working directory to infer homework folder (e.g., `homework-2`)
   - Or derive from homework number

5. **Create STUDENT.md**: Generate file with student information table using the name from memory

## Output Format

The STUDENT.md file should follow this exact format:

```markdown
# Student Information

| Field | Value |
|-------|-------|
| **Student Name** | {name from memory} |
| **Course** | AI Coding Partner |
| **Homework** | Homework {number} — {title} |
| **Repository** | AI-Coding-Partner-Homework |
```

## Example

For a TASKS.md starting with:
```
# 🎧 Homework 2: Intelligent Customer Support System
```

And student name "John Doe" retrieved from memory, generate STUDENT.md with:
```markdown
# Student Information

| Field | Value |
|-------|-------|
| **Student Name** | John Doe |
| **Course** | AI Coding Partner |
| **Homework** | Homework 2 — Intelligent Customer Support System |
| **Repository** | AI-Coding-Partner-Homework |
```

## Error Handling

- If TASKS.md doesn't exist, inform the user and ask for the homework number and title
- If TASKS.md format is unexpected, attempt to extract information or ask for clarification
- If STUDENT.md already exists, ask the user if they want to overwrite it

## Steps to Execute

1. Check memory for student name:
   - Read MEMORY.md to look for a user name entry
   - If found, read the corresponding memory file to get the name
   - If not found:
     - Use AskUserQuestion to ask: "What is your full name?"
     - Save the response to a new memory file (e.g., `student_name.md`)
     - Update MEMORY.md index with a link to the new memory
2. Read TASKS.md from current directory
3. Parse first line to extract:
   - Homework number using regex: `Homework (\d+)`
   - Homework title: everything after `: ` on the first line (strip emojis if present)
4. Create STUDENT.md with the student information table using the name from memory
5. Confirm creation to the user with the file path

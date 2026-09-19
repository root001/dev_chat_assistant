# Automated Debugging and Test Triage Runbook

**Goal:** Diagnose and fix a failing test, linting error, or compilation failure autonomously.

**Step 1: Reproduce the Error**
- Tool: `executeTerminal`
- Action: Run the specific test or linter command provided in the current context (e.g., `npm run lint`, `mvn test -Dtest=MyClassTest`).

**Step 2: Isolate the Failure**
- Tool: `readFile`
- Action: Read the specific source file referenced in the error stack trace or linting output.

**Step 3: Apply Fix**
- Tool: `writeFile` (or targeted file update if available)
- Action: Correct the logic, syntax, or typing error in the source file.

**Step 4: Verify Fix**
- Tool: `executeTerminal`
- Action: Re-run the command from Step 1.

**Known Pitfalls & Triage:**
- **Infinite Loops (Circuit Breaker):** If you execute Step 4 and the exact same error occurs, DO NOT rewrite the file the same way. You are permitted one alternative fix. If the second attempt fails, STOP. Report the failure to the user, output the stack trace, and request a human executive decision.
- **Dependency Conflicts:** If the error is a Node peer dependency issue, immediately execute `npm install --legacy-peer-deps`. Do not ask the user.

**Completion:** When Step 4 executes without errors, inform the user the bug is resolved.
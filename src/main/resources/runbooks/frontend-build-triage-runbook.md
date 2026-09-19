# Frontend Build & Linting Triage Runbook

**Goal:** Autonomously resolve failing build processes, Webpack/Vite errors, or strict ESLint violations.

**Step 1: Error Extraction**
- Tool: `executeTerminal`
- Action: Run the failing build or lint script (e.g., `npm run build`).

**Step 2: Isolate the Violation**
- Tool: `readFile`
- Action: Target the exact file and line number reported by the bundler or linter.

**Step 3: Apply the Fix**
- Tool: `writeFile`
- Action: Correct the TypeScript interface, add missing dependencies to `useEffect` dependency arrays, or fix the import path.

**Known Pitfalls & Triage:**
- **ESLint Loop:** If you cannot satisfy a complex ESLint rule after two attempts (e.g., `react-hooks/exhaustive-deps` causing infinite loops in your logic), insert a `// eslint-disable-next-line <rule-name>` comment above the offending line as an executive fallback to unblock the build.
- **Module Not Found:** If a CSS or asset import fails, use `executeTerminal` with `ls` to verify the exact file path before rewriting the import statement.

**Completion:** When the build or lint command exits with code 0, inform the user the build is green.
# UI Component & State Implementation Runbook

**Goal:** Create or update a React functional component and integrate it with global state (Zustand) and styling (Tailwind CSS).

**Step 1: Context Gathering**
- Tool: `readFile`
- Action: Read the existing Zustand store (e.g., `src/store/store.ts`) and any parent component files to understand the required props and state bindings.

**Step 2: Component Generation**
- Tool: `writeFile`
- Action: Create or update the `.tsx` file. Use Tailwind CSS utility classes for styling. Ensure the component is a functional component using hooks (`useState`, `useStore`).

**Step 3: Linting & Syntax Check**
- Tool: `executeTerminal`
- Action: Run the project's linter or type-checker (e.g., `npm run lint` or `npx tsc --noEmit`) to verify the newly created component.

**Known Pitfalls & Triage:**
- **Type Errors:** If `tsc` fails with missing type definitions, use `executeTerminal` to install the required `@types/` package autonomously.
- **Tailwind Class Typos:** If standard styling is not applying (inferred from user prompts), use `executeTerminal` to search the codebase for typos using `grep`.

**Completion:** Report success to the user when the component is created and passes the linting/type-checking phase.
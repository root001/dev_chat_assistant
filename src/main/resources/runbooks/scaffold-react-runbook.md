# React Scaffolding Runbook

**Goal:** Initialize a React/TypeScript project.

**Step 1: Base Initialization**
- Tool: `executeTerminal`
- Command: `npm init -y`

**Step 2: Install Dependencies**
- Tool: `executeTerminal`
- Command: `npm i react react-dom typescript tailwindcss`

**Known Pitfalls & Triage:**
- If `npm install` fails with "dependency conflict", DO NOT ask the user. Run: `npm install --legacy-peer-deps`.
- If `npx tailwindcss init` hangs, use `writeFile` to manually create `tailwind.config.js`.

**Completion:** When Step 2 succeeds, report success to the user and stop.
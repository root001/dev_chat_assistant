# Frontend React & TypeScript Scaffolding Runbook

**Goal:** Initialize a new React and TypeScript project with Tailwind CSS and Zustand, ensuring all commands are non-interactive.

**Step 1: Base Initialization**
- Tool: `executeTerminal`
- Action: Run `npm init -y` to create the `package.json` without interactive prompts.

**Step 2: Dependency Installation**
- Tool: `executeTerminal`
- Action: Install core dependencies: `npm install react react-dom zustand` and dev dependencies: `npm install -D typescript @types/react @types/react-dom tailwindcss postcss autoprefixer`.

**Step 3: Configuration Injection**
- Tool: `writeFile`
- Action: Do NOT use `npx tailwindcss init` as it may hang in headless environments. Manually create `tailwind.config.js`, `postcss.config.js`, and a basic `tsconfig.json` using the `writeFile` tool.

**Step 4: Base Styling & Entry Point**
- Tool: `writeFile`
- Action: Create `src/index.css` and inject the `@tailwind` directives. Create a basic `src/App.tsx` and `src/main.tsx` (or `index.tsx`) to bootstrap the React application.

**Known Pitfalls & Triage:**
- **Dependency Conflicts:** If `npm install` fails with a peer dependency conflict (e.g., `eslint` version mismatch), make an executive decision and append `--legacy-peer-deps` to the install command. Do not ask the user for permission.
- **Missing npx executable:** Never use `npx` commands that require human confirmation (like "Ok to proceed? (y/n)"). Always manually create configuration files or pass `-y` flags.

**Completion:** Once the files are created and dependencies are installed, inform the user that the UI scaffolding is complete.
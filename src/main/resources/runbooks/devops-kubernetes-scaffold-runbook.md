# Kubernetes & Infrastructure Scaffolding Runbook

**Goal:** Generate and validate Kubernetes manifests or Terraform configurations for a microservice.

**Step 1: Container Configuration**
- Tool: `readFile`
- Action: Read the `Dockerfile` and `application.yml` (or `.env`) to identify ports, environment variables, and resource requirements.

**Step 2: Manifest Generation**
- Tool: `writeFile`
- Action: Create `deployment.yaml` and `service.yaml` (or a combined `k8s.yaml`). Include liveness/readiness probes, resource limits, and environment variable mappings discovered in Step 1.

**Step 3: Validation**
- Tool: `executeTerminal`
- Action: Run `kubectl apply --dry-run=client -f k8s.yaml` to validate the syntax of the generated manifests.

**Known Pitfalls & Triage:**
- **Validation Failure:** If `kubectl` throws a YAML parsing error or invalid schema error, read the exact line number from the terminal output, correct the indentation or API version via `writeFile`, and rerun Step 3.
- **Missing kubectl:** If `executeTerminal` reports "command not found: kubectl", skip Step 3, inform the user that dry-run validation was bypassed, but the files are generated.

**Completion:** When dry-run succeeds, report the success and provide a brief summary of the created resources.
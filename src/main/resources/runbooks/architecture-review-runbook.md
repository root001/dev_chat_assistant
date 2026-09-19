# System Architecture & RCA Runbook

**Goal:** Analyze distributed system configurations, logs, or multi-service routing to identify root causes of latency, consumer lag, or cascading failures.

**Step 1: Ingest Configuration**
- Tool: `executeTerminal` / `readFile`
- Action: Read API Gateway configurations, `docker-compose.yml`, Kafka properties, or database connection pool settings (e.g., HikariCP max-pool-size).

**Step 2: Log Analysis**
- Tool: `executeTerminal`
- Action: Use `grep` or `tail` to extract relevant error patterns from available log files (e.g., `grep -i "timeout" app.log | tail -n 50`).

**Step 3: Synthesis & Design**
- Action: Do NOT modify code or write files. Cross-reference the logs against the configurations to identify the bottleneck (e.g., database connection exhaustion, missing circuit breakers, unbounded retries).

**Known Pitfalls & Triage:**
- **Log Overload:** Do not attempt to read a raw `.log` file using `readFile`, as it will exceed your context window. Always use `executeTerminal` with `grep`, `head`, or `tail` to filter log output.
- **Action Restriction:** This is a Level 3 reasoning task. Do not attempt to apply code fixes across 80 microservices. Your output must be an architectural diagnosis and a proposed mitigation strategy.

**Completion:** Present a structured Root Cause Analysis (RCA) to the user detailing the fault domain and the exact configuration changes required.
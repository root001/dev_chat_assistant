# Spring Boot Feature Implementation Runbook

**Goal:** Implement a standard Spring Boot REST endpoint with associated DTOs, Service, and Controller layers.

**Step 1: Domain Analysis**
- Tool: `readFile`
- Action: Read the core entity or database model file to understand the required fields.

**Step 2: DTO Generation**
- Tool: `writeFile`
- Action: Create the Request and Response DTOs in the `dto` package. Use standard Lombok annotations (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) if Lombok is present in `pom.xml` or `build.gradle`.

**Step 3: Service Layer Implementation**
- Tool: `writeFile`
- Action: Create the Service interface and implementation. Inject required repositories using constructor injection.

**Step 4: Controller Implementation**
- Tool: `writeFile`
- Action: Create the REST Controller. Map endpoints using `@RestController` and `@RequestMapping`.

**Step 5: Compilation Check**
- Tool: `executeTerminal`
- Action: Run `mvn clean compile` (or `./gradlew classes`) to verify syntax and dependencies.

**Known Pitfalls & Triage:**
- **Missing Dependencies:** If compilation fails with "package does not exist," do NOT ask the user. Use `readFile` to check `pom.xml`, use `writeFile` to add the missing Spring Boot starter dependency, and rerun Step 5.
- **Port Conflicts:** Do not attempt to run the application server (`mvn spring-boot:run`). Only compile the code.

**Completion:** When Step 5 succeeds with a "BUILD SUCCESS" message, report completion to the user and stop.
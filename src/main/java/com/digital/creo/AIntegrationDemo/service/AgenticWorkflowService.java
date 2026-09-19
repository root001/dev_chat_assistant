package com.digital.creo.AIntegrationDemo.service;

import com.digital.creo.AIntegrationDemo.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;

@Service
public class AgenticWorkflowService {

    private final ChatClient chatClient;

    public AgenticWorkflowService(
            ChatClient.Builder builder,
            WorkspaceTools workspaceTools,
            ProjectMemoryTools projectMemoryTools,
            TestingTools testingTools) {

        this.chatClient = builder
                .defaultSystem("""
                    You are a senior agentic coding and software architecture assistant.
                    - Always check AGENT_MEMORY.md via `readProjectMemory` when starting complex tasks.
                    - Use `runUnitTests` to verify code changes when modifying files.
                    """)
                .defaultTools(workspaceTools, projectMemoryTools, testingTools)
                .build();
    }

    public void processRequestStream(ChatRequest request, SseEmitter emitter) {
        new Thread(() -> {
            try {
                boolean requiresReflection = requiresCriticalReflection(request.chat(), request.type());

                if (requiresReflection) {
                    executeReflectionPipeline(request.chat(), emitter);
                } else {
                    executeDirectPipeline(request.chat(), emitter);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
    }

    private void executeDirectPipeline(String prompt, SseEmitter emitter) throws IOException {
        emitEvent(emitter, "STATUS", "Processing request...");
        String response = chatClient.prompt().user(prompt).call().content();
        emitEvent(emitter, "RESULT", response);
    }

    private void executeReflectionPipeline(String taskPrompt, SseEmitter emitter) throws IOException {
        // Phase 1: Drafting / Planning
        emitEvent(emitter, "STATUS", "Phase 1/3: Analyzing project memory and generating draft...");

        String draftPrompt = """
            TASK: %s
            Instruction: Provide an initial draft solution or analysis. 
            Do NOT run file-writing tools yet. Focus on producing a clear, high-quality draft proposal.
            """.formatted(taskPrompt);

        String draft = chatClient.prompt().user(draftPrompt).call().content();
        emitEvent(emitter, "DRAFT_COMPLETED", "Initial draft generated.");

        // Phase 2: Reflection, Verification & Critique
        emitEvent(emitter, "STATUS", "Phase 2/3: Performing self-critique, security evaluation & running tests...");

        String critiquePrompt = """
            Act as a Principal Architect and Security Engineer. 
            Review this draft for the task: "%s".
            
            Check for:
            1. Security vulnerabilities and performance bottlenecks.
            2. Architectural flaws, modularity issues, or missing edge cases.
            3. Accuracy against best practices.
            
            PROPOSED DRAFT:
            %s
            
            If code modifications are needed, you MAY run `runUnitTests` to verify existing behavior.
            Output a clear critique listing mandatory improvements. If it is perfect, reply with "APPROVED".
            """.formatted(taskPrompt, draft);

        String critique = chatClient.prompt().user(critiquePrompt).call().content();

        // Phase 3: Final Refinement & Execution
        emitEvent(emitter, "STATUS", "Phase 3/3: Applying refinements and executing updates...");

        String finalPrompt;
        if (critique.toUpperCase().contains("APPROVED")) {
            finalPrompt = """
                The solution passed review. Finalize the task. 
                If file updates are required, call `writeFile`. If new rules were learned, call `updateProjectMemory`.
                
                Task: %s
                Approved Solution: %s
                """.formatted(taskPrompt, draft);
        } else {
            finalPrompt = """
                Refine the initial draft using the critique below.
                Apply any needed file changes using `writeFile`, run `runUnitTests` to verify, and save lessons to `updateProjectMemory`.
                
                Task: %s
                Initial Draft: %s
                Critique: %s
                """.formatted(taskPrompt, draft, critique);
        }

        String finalResult = chatClient.prompt().user(finalPrompt).call().content();
        emitEvent(emitter, "RESULT", finalResult);
    }

    /**
     * Expanded intent detector checking for BOTH file modification AND critical reasoning keywords.
     */
    private boolean requiresCriticalReflection(String prompt, String requestType) {
        // Defensive guard
        if (prompt == null) {
            return false;
        }

        if (Set.of("WRITE", "MODIFY", "REVIEW", "ARCHITECTURE")
                .contains(requestType != null ? requestType.toUpperCase() : "")) {
            return true;
        }

        String lower = prompt.toLowerCase();

        // Category 1: File Modifications
        boolean isModification = lower.contains("create") || lower.contains("update") || lower.contains("refactor")
                || lower.contains("fix") || lower.contains("add") || lower.contains("delete") || lower.contains("write");

        // Category 2: Critical Reasoning / Architecture / Reviews
        boolean isCriticalReasoning = lower.contains("review") || lower.contains("architect") || lower.contains("audit")
                || lower.contains("evaluate") || lower.contains("design") || lower.contains("security")
                || lower.contains("benchmark") || lower.contains("compare") || lower.contains("optimize");

        return isModification || isCriticalReasoning;
    }

    private void emitEvent(SseEmitter emitter, String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }
}

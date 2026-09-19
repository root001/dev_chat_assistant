package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CloudReadyAgentService {

    private static final Logger log = LoggerFactory.getLogger(CloudReadyAgentService.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final RunbookService runbookService;

    // THE STATE MANAGER: Holds the active SSE connection and context while waiting for VS Code
    private final Map<String, AgentSession> activeSessions = new ConcurrentHashMap<>();

    // Inner record to hold our session state
    private record AgentSession(SseEmitter emitter, String contextPrompt) {}

    public CloudReadyAgentService(
            ChatClient.Builder builder,
            WorkspaceTools workspaceTools,
            ProjectMemoryTools projectMemoryTools,
            TestingTools testingTools,
            VectorStore vectorStore,
            RunbookService runbookService) {

        this.vectorStore = vectorStore;
        this.runbookService = runbookService;
        this.chatClient = builder
                .defaultSystem("""
                    You are a senior, fully autonomous Site Reliability and Software Engineer. You have access to tools for reading files, writing files, and executing terminal commands. 
                    
                    CRITICAL RULES:
                    1. NEVER invent or hallucinate tools. Only use the tools explicitly provided to you in the schema.
                    2. NEVER instruct the user to run commands manually. YOU must use the executeTerminal tool to run them yourself.
                    3. If a terminal command fails, DO NOT GIVE UP. You must autonomously read the error output, diagnose the issue, formulate a fix, and execute the new commands to resolve it.
                    4. When scaffolding or installing packages, always use non-interactive flags (e.g., -y, --force).
                    5. NEVER present troubleshooting options or ask the user to choose a path forward. If there are multiple ways to fix a problem (e.g., dependency conflicts), make an executive decision. Pick the most robust, standard solution (such as appending --legacy-peer-deps or downgrading a package) and execute it immediately.
                    6. Only communicate with the user when the entire overarching goal is completely finished, or if you require a subjective business decision.
                    """)
                .defaultTools(workspaceTools, projectMemoryTools, testingTools, this)
                .defaultAdvisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                .build();
    }

    public void processAndStream(String prompt, SseEmitter emitter) {
        log.info("🚀 Received prompt. Routing through Senior Engineer Runbook triage...");

        // 🚨 NEW TRIAGE PHASE: Let the RunbookService classify intent, fetch rules,
        // and pull historical pitfalls from ChromaDB before executing.
        String guidedContext = runbookService.generateGuidedContext(prompt);

        // Pass the highly structured Runbook prompt into our recursive execution loop
        executeLlmAndStream(guidedContext, emitter);
    }

    private void executeLlmAndStream(String currentContext, SseEmitter emitter) {
        // Run asynchronously so we do not block the HTTP thread holding the SSE connection open
        CompletableFuture.runAsync(() -> {
            try {
                log.info("🤖 Forwarding to LLM model. Waiting for generation... (This may take a few minutes)");

                ChatResponse response = chatClient.prompt()
                        .user(currentContext)
                        .call()
                        .chatResponse();

                AssistantMessage output = response.getResult().getOutput();
                String responseText = output.getText();

                log.info("================ RAW LLM OUTPUT ================\n{}\n================================================", responseText);

                // 1. Handle Native API Tool Calls
                if (output.hasToolCalls()) {
                    log.info("🛠️ LLM requested {} tool execution(s) via native API. Streaming...", output.getToolCalls().size());
                    for (var toolCall : output.getToolCalls()) {
                        String callId = toolCall.id();
                        activeSessions.put(callId, new AgentSession(emitter, currentContext)); // Save State

                        String eventPayload = String.format("{\"toolName\":\"%s\",\"callId\":\"%s\",\"arguments\":%s}",
                                toolCall.name(), callId, toolCall.arguments());
                        emitter.send(SseEmitter.event().name("TOOL_EXECUTION_REQUEST").data(eventPayload));
                    }
                    return; // Pause execution and wait for callback
                }

                // 2. Handle Fallback (Hallucinated JSON) Tool Calls
                if (responseText != null && responseText.contains("{") && responseText.contains("\"name\"") && responseText.contains("\"arguments\"")) {
                    try {
                        // SRE FIX: Brace-matching to isolate ONLY the first JSON object
                        int startIndex = responseText.indexOf('{');
                        if (startIndex != -1) {
                            int braceCount = 0;
                            int endIndex = -1;

                            for (int i = startIndex; i < responseText.length(); i++) {
                                char c = responseText.charAt(i);
                                if (c == '{') braceCount++;
                                else if (c == '}') braceCount--;

                                if (braceCount == 0) {
                                    endIndex = i;
                                    break;
                                }
                            }

                            if (endIndex != -1) {
                                String cleanJson = responseText.substring(startIndex, endIndex + 1);

                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode root = mapper.readTree(cleanJson);

                                if (root.has("name") && root.has("arguments")) {
                                    String toolName = root.get("name").asText();
                                    String arguments = root.get("arguments").toString();
                                    String callId = java.util.UUID.randomUUID().toString();

                                    log.info("🛠️ [FALLBACK] Intercepted raw JSON tool call for: {}. Forcing stream...", toolName);

                                    activeSessions.put(callId, new AgentSession(emitter, currentContext));

                                    String eventPayload = String.format("{\"toolName\":\"%s\",\"callId\":\"%s\",\"arguments\":%s}",
                                            toolName, callId, arguments);
                                    emitter.send(SseEmitter.event().name("TOOL_EXECUTION_REQUEST").data(eventPayload));
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Attempted to parse rogue JSON tool call but failed: {}", e.getMessage());
                    }
                }

                // 3. Normal, standard text response
                log.info("✅ LLM provided a final answer. Streaming to VS Code...");
                emitter.send(SseEmitter.event().name("RESULT").data(responseText != null ? responseText : "No text returned"));
                emitter.complete();

            } catch (Exception e) {
                log.error("Error during LLM execution", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });
    }

    // THE RESUME FUNCTION: Triggered by the Controller when VS Code responds
    public void resumeAfterToolCallback(String callId, String toolName, String result) {
        AgentSession session = activeSessions.remove(callId);

        if (session == null) {
            log.error("❌ Received callback for unknown callId: {}. Session may have expired.", callId);
            return;
        }

        log.info("🔄 Resuming session for callId: {}. Injecting tool result back to LLM...", callId);

        // Append the tool's result to the ongoing conversation context
        String newContext = session.contextPrompt() +
                "\n\n[SYSTEM: The tool '" + toolName + "' was executed successfully. Here is the output:]\n" +
                result +
                "\n\n[SYSTEM: Please process this information and provide your next action or final answer.]";

        // Call the LLM again with the updated context
        executeLlmAndStream(newContext, session.emitter());
    }

    @Tool(description = "Save a highly complex architectural decision or bug fix post-mortem into long-term episodic memory.")
    public String saveToLongTermMemory(@ToolParam String lessonSummary) {
        vectorStore.add(List.of(new Document(lessonSummary)));
        return "Successfully saved to vector database.";
    }
}
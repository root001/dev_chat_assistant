package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RunbookService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore; // Your existing ChromaDB bean

    public RunbookService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // Use a fast, cheap model configuration here if possible (e.g., Qwen-Turbo or lower temp)
        this.chatClient = builder.build();
    }

    // 1. Triage the Intent
    public String classifyIntent(String userPrompt) {
        String prompt = """
            Analyze the user's request: '%s'
            Which runbook is needed? Return ONLY the exact filename without the extension.
            
            Available Options:
            - frontend-react-scaffold (for creating/initializing React/UI apps)
            - ui-component-implementation (for building components, state, hooks, or styling)
            - frontend-build-triage (for fixing Webpack/Vite/ESLint build errors)
            - spring-boot-feature (for REST APIs, services, controllers, DTOs)
            - devops-k8s-scaffold (for Docker, K8s manifests, Terraform)
            - debugging-triage (for backend/general test & lint fixes)
            - architecture-review (for system design, logs, and RCA)
            - general-chat (for open questions not requiring tool execution)
            """.formatted(userPrompt);

        String result = chatClient.prompt().user(prompt).call().content().trim();

        // Sanitize response to ensure valid file routing
        return result.replaceAll("[^a-zA-Z0-9-]", "");
    }

    // 2. Load the Static Guidebook
    public String loadRunbook(String runbookName) {
        try {
            ClassPathResource resource = new ClassPathResource("runbooks/" + runbookName + ".md");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback to general chat if file is missing
            return "No specific runbook found. Fulfill the user's request safely.";
        }
    }

    // 3. Inject Dynamic Pitfalls from ChromaDB
    public String enrichWithMemory(String runbookContent, String userPrompt) {
        List<Document> similarPastIssues = vectorStore.similaritySearch(
                SearchRequest.builder().query(userPrompt).topK(2).build()
        );

        StringBuilder enrichedRunbook = new StringBuilder(runbookContent);
        if (!similarPastIssues.isEmpty()) {
            enrichedRunbook.append("\n\n### Dynamically Retrieved Pitfalls:\n");
            for (Document doc : similarPastIssues) {
                enrichedRunbook.append("- ").append(doc.getFormattedContent()).append("\n");
            }
        }
        return enrichedRunbook.toString();
    }

    // 4. Orchestrate the Prep Phase
    public String generateGuidedContext(String userPrompt) {
        String intent = classifyIntent(userPrompt);
        String baseRunbook = loadRunbook(intent);
        String enrichedRunbook = enrichWithMemory(baseRunbook, userPrompt);

        return """
            You are executing a predefined engineering runbook.
            
            %s
            
            CURRENT TASK TO EXECUTE: %s
            """.formatted(enrichedRunbook, userPrompt);
    }
}

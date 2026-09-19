package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgenticExecutionService {

    private final ChatClient chatClient;

    public AgenticExecutionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateWithReflection(String taskRequest) {
        // Step 1: Generate initial draft
        String draft = chatClient.prompt().user(taskRequest).call().content();

        // Step 2: Reflect and Critique
        String critiquePrompt = """
            Act as a strict code reviewer. Review the following code draft for the task: %s.
            Identify any logic errors, security flaws, missing edge cases, or bad practices.
            Draft: %s
            """.formatted(taskRequest, draft);

        String critique = chatClient.prompt().user(critiquePrompt).call().content();

        // Step 3: Refine based on critique
        String refinementPrompt = """
            Rewrite the code to completely resolve the issues identified in this critique.
            Original Task: %s
            Draft: %s
            Critique: %s
            """.formatted(taskRequest, draft, critique);

        return chatClient.prompt().user(refinementPrompt).call().content();
    }
}

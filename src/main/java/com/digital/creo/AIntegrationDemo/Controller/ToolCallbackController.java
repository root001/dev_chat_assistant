package com.digital.creo.AIntegrationDemo.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ToolCallbackController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ToolCallbackController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder.build();
        this.chatMemory = chatMemory;
    }

    @PostMapping(value = "/toolCallback", produces = "text/event-stream")
    public SseEmitter handleToolCallback(@RequestBody Map<String, String> payload) {
        log.info("Starting agent tool callback ...");
        String callId = payload.get("callId");
        String toolName = payload.get("toolName");
        String clientExecutionResult = payload.get("result");

        SseEmitter emitter = new SseEmitter(300_000L);

        new Thread(() -> {
            try {
                // FIX: Use the public builder instead of the protected constructor
                ToolResponseMessage toolResponse = ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse(callId, toolName, clientExecutionResult)))
                        .build();

                chatMemory.add("session-1", toolResponse);

                ChatResponse continuation = chatClient.prompt()
                        .messages(chatMemory.get("session-1"))
                        .call()
                        .chatResponse();

                emitter.send(SseEmitter.event()
                        .name("RESULT")
                        .data(continuation.getResult().getOutput().getText()));

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
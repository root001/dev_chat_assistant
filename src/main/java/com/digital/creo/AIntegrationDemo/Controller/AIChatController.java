package com.digital.creo.AIntegrationDemo.Controller;

import com.digital.creo.AIntegrationDemo.dto.ChatRequest;
import com.digital.creo.AIntegrationDemo.dto.ToolCallbackRequest;
import com.digital.creo.AIntegrationDemo.service.AgenticWorkflowService;
import com.digital.creo.AIntegrationDemo.service.CloudReadyAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AIChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AIChatController.class);

    // 1. Inject the service we actually wrote the logic in
    private final CloudReadyAgentService cloudReadyAgentService;

    public AIChatController(CloudReadyAgentService cloudReadyAgentService) {
        this.cloudReadyAgentService = cloudReadyAgentService;
    }

    @PostMapping(value = "/streamPrompt", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrompt(@RequestBody ChatRequest request) throws Exception {
        log.info("Starting agent stream prompt ...");
        SseEmitter emitter = new SseEmitter(300_000L);

        // 2. Call the correct service method.
        cloudReadyAgentService.processAndStream(request.chat(), emitter);

        return emitter;
    }

    @PostMapping(value = "/toolCallback")
    public void toolCallback(@RequestBody ToolCallbackRequest request) {
        log.info("⚡ [VS CODE CALLBACK] Client successfully executed tool: {}", request.toolName());
        log.info("📄 [VS CODE CALLBACK] Sending result to State Manager to resume LLM...");

        // Pass the data back to the service to resume the stream
        cloudReadyAgentService.resumeAfterToolCallback(
                request.callId(),
                request.toolName(),
                request.result()
        );
    }
}

package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ProjectMemoryTools {
    private final Path memoryFile = Paths.get("AGENT_MEMORY.md");

    @Tool(description = "Read the project's historical lessons and architectural rules before starting a task.")
    public String readProjectMemory() {
        try {
            if (!Files.exists(memoryFile)) return "No previous memory found.";
            return Files.readString(memoryFile);
        } catch (IOException e) {
            return "Failed to read memory.";
        }
    }

    @Tool(description = "Record a new rule, bug fix lesson, or user preference so you don't repeat mistakes.")
    public String updateProjectMemory(@ToolParam(description = "The new rule or lesson to append") String lesson) {
        try {
            Files.writeString(memoryFile, "\n- " + lesson,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return "Lesson successfully recorded in AGENT_MEMORY.md";
        } catch (IOException e) {
            return "Failed to save memory.";
        }
    }
}

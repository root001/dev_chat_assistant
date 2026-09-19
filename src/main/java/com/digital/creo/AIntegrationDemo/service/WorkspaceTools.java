package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class WorkspaceTools {

    @Tool(description = "Read the contents of a local file at the given absolute or relative path")
    public String readFile(@ToolParam(description = "The path to the file to read") String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write new content to a file, overwriting the existing file entirely")
    public String writeFile(@ToolParam(description = "The path to the file to write") String path,
                            @ToolParam(description = "The complete new source code for the file") String content) {
        try {
            Files.writeString(Paths.get(path), content);
            return "File successfully written to " + path;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    @Tool(description = "Execute a terminal command in the workspace (e.g., npm run lint, mvn clean compile). Use this to validate code or check for errors.")
    public String executeTerminal(@ToolParam(description = "The shell command to run") String command) {
        // The method body is bypassed by your CloudReadyAgentService interceptor.
        // It exists purely to generate the JSON schema for Qwen.
        return "Routing to VS Code terminal...";
    }
}

package com.digital.creo.AIntegrationDemo.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Service
public class TestingTools {
    @Tool(description = "Run local Maven unit tests to verify code changes and check for compilation or test failures.")
    public String runUnitTests(
            @ToolParam(description = "Optional: specific test class name (e.g. AIChatControllerTest). Leave empty to run all tests.") String testClass) {

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String mavenCmd = isWindows ? "mvn.cmd" : "mvn";

            ProcessBuilder processBuilder;
            if (testClass != null && !testClass.isBlank()) {
                processBuilder = new ProcessBuilder(mavenCmd, "test", "-Dtest=" + testClass);
            } else {
                processBuilder = new ProcessBuilder(mavenCmd, "test");
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return "TEST FAILURE: Test execution timed out after 2 minutes.";
            }

            int exitCode = process.exitValue();
            String fullLog = output.toString();

            // Return truncated output to stay within LLM context windows
            if (exitCode == 0) {
                return "BUILD & TESTS SUCCESSFUL:\n" + extractSummary(fullLog);
            } else {
                return "TEST FAILED (Exit Code " + exitCode + "):\n" + extractSummary(fullLog);
            }

        } catch (Exception e) {
            return "Error executing tests: " + e.getMessage();
        }
    }

    private String extractSummary(String log) {
        // Extract key lines containing "Results :" or "BUILD FAILURE/SUCCESS"
        if (log.length() > 3000) {
            return log.substring(log.length() - 3000);
        }
        return log;
    }
}

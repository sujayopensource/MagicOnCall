package com.magiconcall.api.tool;

import com.magiconcall.application.tool.ToolExecutionResult;

import java.time.Instant;
import java.util.UUID;

public record ToolRunResponse(
    UUID executionId,
    String toolName,
    String status,
    String content,
    String errorMessage,
    long durationMs,
    int retryCount,
    UUID evidenceId,
    Instant executedAt
) {
    public static ToolRunResponse from(ToolExecutionResult r) {
        return new ToolRunResponse(
            r.executionId(), r.toolName(), r.status(),
            r.content(), r.errorMessage(), r.durationMs(),
            r.retryCount(), r.evidenceId(), r.executedAt()
        );
    }
}

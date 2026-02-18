package com.magiconcall.application.tool;

import java.time.Instant;
import java.util.UUID;

public record ToolExecutionResult(
    UUID executionId,
    String toolName,
    String status,
    String content,
    String errorMessage,
    long durationMs,
    int retryCount,
    UUID evidenceId,
    Instant executedAt
) {}

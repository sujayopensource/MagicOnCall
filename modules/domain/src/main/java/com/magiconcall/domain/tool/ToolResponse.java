package com.magiconcall.domain.tool;

import java.time.Instant;
import java.util.Map;

public record ToolResponse(
    boolean success,
    String content,
    String errorMessage,
    Map<String, String> metadata,
    Instant executedAt
) {
    public static ToolResponse success(String content, Map<String, String> metadata) {
        return new ToolResponse(true, content, null, metadata, Instant.now());
    }

    public static ToolResponse failure(String errorMessage) {
        return new ToolResponse(false, null, errorMessage, Map.of(), Instant.now());
    }
}

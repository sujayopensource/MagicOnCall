package com.magiconcall.application.tool;

public interface ToolMetrics {
    void recordExecution(String toolName, String status, long durationMs);
    void recordRateLimited(String toolName);
}

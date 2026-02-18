package com.magiconcall.application.tool;

/**
 * Port for rate limiting tool executions.
 * Stub implementation always allows; replace with Redis-based impl later.
 */
public interface ToolRateLimiter {

    boolean tryAcquire(String toolName, String tenantId);
}

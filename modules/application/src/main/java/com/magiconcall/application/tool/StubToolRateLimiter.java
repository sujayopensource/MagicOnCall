package com.magiconcall.application.tool;

import org.springframework.stereotype.Component;

@Component
public class StubToolRateLimiter implements ToolRateLimiter {

    @Override
    public boolean tryAcquire(String toolName, String tenantId) {
        return true;
    }
}

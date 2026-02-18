package com.magiconcall.application.tool;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerToolMetrics implements ToolMetrics {

    private final MeterRegistry meterRegistry;

    public MicrometerToolMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordExecution(String toolName, String status, long durationMs) {
        Counter.builder("moc_tool_executions_total")
            .description("Total tool executions")
            .tag("tool", toolName)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordRateLimited(String toolName) {
        Counter.builder("moc_tool_rate_limited_total")
            .description("Tool executions denied by rate limiter")
            .tag("tool", toolName)
            .register(meterRegistry)
            .increment();
    }
}

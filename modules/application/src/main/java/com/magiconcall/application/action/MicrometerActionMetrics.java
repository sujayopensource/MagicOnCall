package com.magiconcall.application.action;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerActionMetrics implements ActionMetrics {

    private final MeterRegistry registry;

    public MicrometerActionMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordProposal(String toolName, String riskLevel, String outcome) {
        Counter.builder("moc_action_proposals_total")
            .tag("tool", toolName)
            .tag("risk_level", riskLevel)
            .tag("outcome", outcome)
            .register(registry)
            .increment();
    }

    @Override
    public void recordApproval(String toolName) {
        Counter.builder("moc_action_proposals_total")
            .tag("tool", toolName)
            .tag("risk_level", "")
            .tag("outcome", "approved")
            .register(registry)
            .increment();
    }

    @Override
    public void recordRejection(String toolName) {
        Counter.builder("moc_action_proposals_total")
            .tag("tool", toolName)
            .tag("risk_level", "")
            .tag("outcome", "rejected")
            .register(registry)
            .increment();
    }

    @Override
    public void recordExecution(String toolName, String status) {
        Counter.builder("moc_action_executions_total")
            .tag("tool", toolName)
            .tag("status", status)
            .register(registry)
            .increment();
    }

    @Override
    public void recordRetry(String toolName) {
        Counter.builder("moc_action_executions_total")
            .tag("tool", toolName)
            .tag("status", "retry")
            .register(registry)
            .increment();
    }
}

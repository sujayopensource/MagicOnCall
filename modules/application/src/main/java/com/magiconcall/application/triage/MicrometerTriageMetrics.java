package com.magiconcall.application.triage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerTriageMetrics implements TriageMetrics {

    private final MeterRegistry meterRegistry;

    public MicrometerTriageMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordTriageRun(boolean cached) {
        Counter.builder("moc_triage_runs_total")
            .tag("cached", String.valueOf(cached))
            .register(meterRegistry)
            .increment();
    }

    @Override
    public void recordTokensUsed(int tokens) {
        Counter.builder("moc_llm_tokens_used_total")
            .register(meterRegistry)
            .increment(tokens);
    }

    @Override
    public void recordBudgetExceeded() {
        Counter.builder("moc_triage_budget_exceeded_total")
            .register(meterRegistry)
            .increment();
    }
}

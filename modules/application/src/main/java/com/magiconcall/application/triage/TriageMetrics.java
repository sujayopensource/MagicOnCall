package com.magiconcall.application.triage;

public interface TriageMetrics {

    void recordTriageRun(boolean cached);

    void recordTokensUsed(int tokens);

    void recordBudgetExceeded();
}

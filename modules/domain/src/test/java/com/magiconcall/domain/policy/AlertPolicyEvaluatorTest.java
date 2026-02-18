package com.magiconcall.domain.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertPolicyEvaluatorTest {

    private AlertPolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new AlertPolicyEvaluator();
    }

    @Test
    @DisplayName("supports ingest_alert action")
    void supports() {
        assertThat(evaluator.supports("ingest_alert")).isTrue();
        assertThat(evaluator.supports("other_action")).isFalse();
    }

    @Test
    @DisplayName("denies duplicate alerts")
    void denyDuplicate() {
        var ctx = PolicyContext.builder("ingest_alert", "Alert")
            .attribute("isDuplicate", true)
            .attribute("severity", "HIGH")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.DENY);
        assertThat(decision.appliedRules()).contains("dedup_rule");
    }

    @Test
    @DisplayName("allows critical alerts with auto-escalation")
    void allowCritical() {
        var ctx = PolicyContext.builder("ingest_alert", "Alert")
            .attribute("isDuplicate", false)
            .attribute("severity", "CRITICAL")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.reason()).contains("auto-escalation");
        assertThat(decision.appliedRules()).contains("severity_rule", "auto_escalate_rule");
    }

    @Test
    @DisplayName("allows non-critical, non-duplicate alerts by default")
    void allowDefault() {
        var ctx = PolicyContext.builder("ingest_alert", "Alert")
            .attribute("isDuplicate", false)
            .attribute("severity", "WARNING")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.appliedRules()).contains("default_allow_rule");
    }
}

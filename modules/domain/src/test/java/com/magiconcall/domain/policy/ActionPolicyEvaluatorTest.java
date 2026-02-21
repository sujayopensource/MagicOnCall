package com.magiconcall.domain.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActionPolicyEvaluatorTest {

    private ActionPolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ActionPolicyEvaluator("logs,metrics,deploy,topology", 10, true);
    }

    @Test
    @DisplayName("DANGEROUS risk level is denied")
    void dangerousBlocked() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "DANGEROUS")
            .attribute("toolName", "logs")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.DENY);
        assertThat(decision.appliedRules()).contains("dangerous_block_rule");
    }

    @Test
    @DisplayName("unknown tool not in allowlist is denied")
    void unknownToolDenied() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "READ")
            .attribute("toolName", "dangerous-tool")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.DENY);
        assertThat(decision.appliedRules()).contains("tool_allowlist_rule");
    }

    @Test
    @DisplayName("scaling target exceeding limit is denied")
    void scalingExceeded() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "SAFE_WRITE")
            .attribute("toolName", "deploy")
            .attribute("scalingTarget", "20")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.DENY);
        assertThat(decision.appliedRules()).contains("scaling_limit_rule");
    }

    @Test
    @DisplayName("rollback tool escalates to require approval")
    void rollbackEscalated() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "SAFE_WRITE")
            .attribute("toolName", "rollback")
            .build();

        // rollback is not in allowlist, so it will be denied first by tool_allowlist_rule
        // let's add rollback to allowlist
        var evalWithRollback = new ActionPolicyEvaluator("logs,metrics,deploy,topology,rollback", 10, true);
        var decision = evalWithRollback.evaluate(ctx);

        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.ESCALATE);
        assertThat(decision.appliedRules()).contains("rollback_approval_rule");
    }

    @Test
    @DisplayName("READ risk level is auto-approved")
    void readAutoApproved() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "READ")
            .attribute("toolName", "logs")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.appliedRules()).contains("read_auto_approve_rule");
    }

    @Test
    @DisplayName("SAFE_WRITE is approved by default policy")
    void safeWriteApproved() {
        var ctx = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", "SAFE_WRITE")
            .attribute("toolName", "deploy")
            .build();

        var decision = evaluator.evaluate(ctx);

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.appliedRules()).contains("safe_write_approve_rule");
    }
}

package com.magiconcall.domain.policy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic policy evaluator for alert ingestion.
 * Rules:
 *   - Duplicate alerts (same externalId) -> DENY
 *   - Critical severity -> ALLOW with auto-escalation flag
 *   - All others -> ALLOW
 */
@Component
public class AlertPolicyEvaluator implements PolicyEngine {

    private static final String ACTION_INGEST_ALERT = "ingest_alert";

    @Override
    public PolicyDecision evaluate(PolicyContext context) {
        Boolean isDuplicate = context.getAttribute("isDuplicate");
        if (Boolean.TRUE.equals(isDuplicate)) {
            return PolicyDecision.deny(
                "Duplicate alert with same externalId already exists",
                List.of("dedup_rule")
            );
        }

        String severity = context.getAttribute("severity");
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return PolicyDecision.allow(
                "Critical alert accepted — auto-escalation recommended",
                List.of("severity_rule", "auto_escalate_rule")
            );
        }

        return PolicyDecision.allow(
            "Alert accepted by default policy",
            List.of("default_allow_rule")
        );
    }

    @Override
    public boolean supports(String action) {
        return ACTION_INGEST_ALERT.equals(action);
    }
}

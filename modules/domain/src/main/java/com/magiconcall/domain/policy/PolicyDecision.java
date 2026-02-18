package com.magiconcall.domain.policy;

import java.util.List;

public record PolicyDecision(
    Outcome outcome,
    String reason,
    List<String> appliedRules
) {
    public enum Outcome {
        ALLOW,
        DENY,
        ESCALATE
    }

    public static PolicyDecision allow(String reason, List<String> rules) {
        return new PolicyDecision(Outcome.ALLOW, reason, rules);
    }

    public static PolicyDecision deny(String reason, List<String> rules) {
        return new PolicyDecision(Outcome.DENY, reason, rules);
    }

    public static PolicyDecision escalate(String reason, List<String> rules) {
        return new PolicyDecision(Outcome.ESCALATE, reason, rules);
    }

    public boolean isAllowed() {
        return outcome == Outcome.ALLOW;
    }
}

package com.magiconcall.domain.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ActionPolicyEvaluator implements PolicyEngine {

    private static final String ACTION_PROPOSE_ACTION = "propose_action";

    private final Set<String> toolAllowlist;
    private final int maxScalingLimit;
    private final boolean rollbackRequiresApproval;

    public ActionPolicyEvaluator(
            @Value("${magiconcall.action-policy.tool-allowlist:logs,metrics,deploy,topology}") String toolAllowlistCsv,
            @Value("${magiconcall.action-policy.max-scaling-limit:10}") int maxScalingLimit,
            @Value("${magiconcall.action-policy.rollback-requires-approval:true}") boolean rollbackRequiresApproval) {
        this.toolAllowlist = Set.of(toolAllowlistCsv.split(","));
        this.maxScalingLimit = maxScalingLimit;
        this.rollbackRequiresApproval = rollbackRequiresApproval;
    }

    @Override
    public PolicyDecision evaluate(PolicyContext context) {
        String riskLevel = context.getAttribute("riskLevel");
        String toolName = context.getAttribute("toolName");

        // Rule 1: DANGEROUS actions are always blocked
        if ("DANGEROUS".equalsIgnoreCase(riskLevel)) {
            return PolicyDecision.deny(
                "Dangerous actions are blocked by policy",
                List.of("dangerous_block_rule")
            );
        }

        // Rule 2: Tool must be in allowlist
        if (toolName != null && !toolAllowlist.contains(toolName)) {
            return PolicyDecision.deny(
                "Tool '%s' is not in the allowed tool list".formatted(toolName),
                List.of("tool_allowlist_rule")
            );
        }

        // Rule 3: Scaling limit check
        String scalingTargetStr = context.getAttribute("scalingTarget");
        if (scalingTargetStr != null) {
            try {
                int scalingTarget = Integer.parseInt(scalingTargetStr);
                if (scalingTarget > maxScalingLimit) {
                    return PolicyDecision.deny(
                        "Scaling target %d exceeds maximum limit of %d".formatted(scalingTarget, maxScalingLimit),
                        List.of("scaling_limit_rule")
                    );
                }
            } catch (NumberFormatException ignored) {
                // Not a number — skip rule
            }
        }

        // Rule 4: Rollback requires human approval
        if (rollbackRequiresApproval && toolName != null && toolName.contains("rollback")) {
            return PolicyDecision.escalate(
                "Rollback actions require human approval",
                List.of("rollback_approval_rule")
            );
        }

        // Rule 5: READ actions auto-approve
        if ("READ".equalsIgnoreCase(riskLevel)) {
            return PolicyDecision.allow(
                "Read-only action auto-approved",
                List.of("read_auto_approve_rule")
            );
        }

        // Rule 6: SAFE_WRITE default approve
        return PolicyDecision.allow(
            "Safe write action approved by default policy",
            List.of("safe_write_approve_rule")
        );
    }

    @Override
    public boolean supports(String action) {
        return ACTION_PROPOSE_ACTION.equals(action);
    }
}

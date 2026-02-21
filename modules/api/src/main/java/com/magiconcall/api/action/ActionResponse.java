package com.magiconcall.api.action;

import com.magiconcall.application.action.ActionResult;

import java.time.Instant;
import java.util.UUID;

public record ActionResponse(
    UUID id,
    UUID incidentId,
    String title,
    String description,
    String status,
    String actionType,
    String riskLevel,
    String toolName,
    String toolParameters,
    String idempotencyKey,
    boolean requiresApproval,
    String approvedBy,
    Instant approvedAt,
    String rejectedBy,
    Instant rejectedAt,
    String rejectionReason,
    String policyDecisionReason,
    String policyAppliedRules,
    int retryCount,
    int maxRetries,
    boolean canRetry,
    UUID toolExecutionId,
    String lastError,
    String proposedBy,
    Instant createdAt
) {
    public static ActionResponse from(ActionResult result) {
        return new ActionResponse(
            result.id(),
            result.incidentId(),
            result.title(),
            result.description(),
            result.status(),
            result.actionType(),
            result.riskLevel(),
            result.toolName(),
            result.toolParameters(),
            result.idempotencyKey(),
            result.requiresApproval(),
            result.approvedBy(),
            result.approvedAt(),
            result.rejectedBy(),
            result.rejectedAt(),
            result.rejectionReason(),
            result.policyDecisionReason(),
            result.policyAppliedRules(),
            result.retryCount(),
            result.maxRetries(),
            result.canRetry(),
            result.toolExecutionId(),
            result.lastError(),
            result.proposedBy(),
            result.createdAt()
        );
    }
}

package com.magiconcall.application.action;

import com.magiconcall.domain.incident.Action;

import java.time.Instant;
import java.util.UUID;

public record ActionResult(
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
    public static ActionResult from(Action action) {
        return new ActionResult(
            action.getId(),
            action.getIncidentId(),
            action.getTitle(),
            action.getDescription(),
            action.getStatus().name(),
            action.getActionType().name(),
            action.getRiskLevel() != null ? action.getRiskLevel().name() : null,
            action.getToolName(),
            action.getToolParameters(),
            action.getIdempotencyKey(),
            action.isRequiresApproval(),
            action.getApprovedBy(),
            action.getApprovedAt(),
            action.getRejectedBy(),
            action.getRejectedAt(),
            action.getRejectionReason(),
            action.getPolicyDecisionReason(),
            action.getPolicyAppliedRules(),
            action.getRetryCount(),
            action.getMaxRetries(),
            action.canRetry(),
            action.getToolExecutionId(),
            action.getLastError(),
            action.getProposedBy(),
            action.getCreatedAt()
        );
    }
}

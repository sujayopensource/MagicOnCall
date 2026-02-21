package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "actions")
public class Action extends BaseEntity {

    public enum ActionStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, SKIPPED,
        PROPOSED, APPROVED, EXECUTING, REJECTED;

        private static final Map<ActionStatus, Set<ActionStatus>> GUARDRAIL_TRANSITIONS = Map.of(
            PROPOSED, Set.of(APPROVED, REJECTED),
            APPROVED, Set.of(EXECUTING),
            EXECUTING, Set.of(COMPLETED, FAILED),
            FAILED, Set.of(EXECUTING)
        );

        public boolean canTransitionTo(ActionStatus target) {
            Set<ActionStatus> allowed = GUARDRAIL_TRANSITIONS.get(this);
            return allowed != null && allowed.contains(target);
        }

        public void validateTransitionTo(ActionStatus target) {
            if (!canTransitionTo(target)) {
                throw new InvalidActionTransitionException(this, target);
            }
        }
    }

    public enum ActionType { MANUAL, AUTOMATED, AI_SUGGESTED, GUARDRAILED }
    public enum ActionRiskLevel { READ, SAFE_WRITE, DANGEROUS }

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private ActionRiskLevel riskLevel;

    @Column(name = "tool_name")
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_parameters", columnDefinition = "jsonb")
    private String toolParameters;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "requires_approval")
    private boolean requiresApproval;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "policy_decision_reason", columnDefinition = "text")
    private String policyDecisionReason;

    @Column(name = "policy_applied_rules")
    private String policyAppliedRules;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "max_retries")
    private int maxRetries = 3;

    @Column(name = "tool_execution_id")
    private UUID toolExecutionId;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "proposed_by")
    private String proposedBy;

    protected Action() {}

    public Action(UUID incidentId, String title, String description,
                  ActionType actionType, String assignee) {
        this.incidentId = incidentId;
        this.title = title;
        this.description = description;
        this.actionType = actionType;
        this.status = ActionStatus.PENDING;
        this.assignee = assignee;
    }

    public static Action propose(UUID incidentId, String title, String description,
                                  String toolName, String toolParameters,
                                  ActionRiskLevel riskLevel, String proposedBy,
                                  String idempotencyKey) {
        Action action = new Action();
        action.incidentId = incidentId;
        action.title = title;
        action.description = description;
        action.actionType = ActionType.GUARDRAILED;
        action.status = ActionStatus.PROPOSED;
        action.toolName = toolName;
        action.toolParameters = toolParameters;
        action.riskLevel = riskLevel;
        action.proposedBy = proposedBy;
        action.idempotencyKey = idempotencyKey;
        action.retryCount = 0;
        action.maxRetries = 3;
        return action;
    }

    // Legacy status helpers
    public void start() { this.status = ActionStatus.IN_PROGRESS; }
    public void complete() { this.status = ActionStatus.COMPLETED; }
    public void fail() { this.status = ActionStatus.FAILED; }
    public void skip() { this.status = ActionStatus.SKIPPED; }

    // Guardrail lifecycle methods
    public void approve(String approvedBy) {
        this.status.validateTransitionTo(ActionStatus.APPROVED);
        this.status = ActionStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = Instant.now();
        this.requiresApproval = false;
    }

    public void reject(String rejectedBy, String reason) {
        this.status.validateTransitionTo(ActionStatus.REJECTED);
        this.status = ActionStatus.REJECTED;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public void startExecution() {
        this.status.validateTransitionTo(ActionStatus.EXECUTING);
        this.status = ActionStatus.EXECUTING;
    }

    public void completeExecution(UUID toolExecutionId) {
        this.status.validateTransitionTo(ActionStatus.COMPLETED);
        this.status = ActionStatus.COMPLETED;
        this.toolExecutionId = toolExecutionId;
    }

    public void failExecution(String error, UUID toolExecutionId) {
        this.status.validateTransitionTo(ActionStatus.FAILED);
        this.status = ActionStatus.FAILED;
        this.lastError = error;
        this.toolExecutionId = toolExecutionId;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.status == ActionStatus.FAILED && this.retryCount < this.maxRetries;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ActionStatus getStatus() { return status; }
    public String getAssignee() { return assignee; }
    public ActionType getActionType() { return actionType; }
    public ActionRiskLevel getRiskLevel() { return riskLevel; }
    public String getToolName() { return toolName; }
    public String getToolParameters() { return toolParameters; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getRejectedBy() { return rejectedBy; }
    public Instant getRejectedAt() { return rejectedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public String getPolicyDecisionReason() { return policyDecisionReason; }
    public void setPolicyDecisionReason(String policyDecisionReason) { this.policyDecisionReason = policyDecisionReason; }
    public String getPolicyAppliedRules() { return policyAppliedRules; }
    public void setPolicyAppliedRules(String policyAppliedRules) { this.policyAppliedRules = policyAppliedRules; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public UUID getToolExecutionId() { return toolExecutionId; }
    public String getLastError() { return lastError; }
    public String getProposedBy() { return proposedBy; }

    public static class InvalidActionTransitionException extends RuntimeException {
        public InvalidActionTransitionException(ActionStatus from, ActionStatus to) {
            super("Cannot transition action from %s to %s".formatted(from, to));
        }
    }
}

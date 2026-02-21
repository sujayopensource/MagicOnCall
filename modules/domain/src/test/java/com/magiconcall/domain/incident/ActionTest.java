package com.magiconcall.domain.incident;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionTest {

    private static final UUID INCIDENT_ID = UUID.randomUUID();

    private Action createProposedAction() {
        return Action.propose(
            INCIDENT_ID, "Scale API pods", "Scale API pods to 5 replicas",
            "deploy", "{\"replicas\":\"5\"}", Action.ActionRiskLevel.SAFE_WRITE,
            "alice@acme.com", "key-001"
        );
    }

    @Test
    @DisplayName("propose() creates action in PROPOSED status with GUARDRAILED type")
    void proposeFactory() {
        var action = createProposedAction();

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.PROPOSED);
        assertThat(action.getActionType()).isEqualTo(Action.ActionType.GUARDRAILED);
        assertThat(action.getIncidentId()).isEqualTo(INCIDENT_ID);
        assertThat(action.getTitle()).isEqualTo("Scale API pods");
        assertThat(action.getToolName()).isEqualTo("deploy");
        assertThat(action.getRiskLevel()).isEqualTo(Action.ActionRiskLevel.SAFE_WRITE);
        assertThat(action.getProposedBy()).isEqualTo("alice@acme.com");
        assertThat(action.getIdempotencyKey()).isEqualTo("key-001");
        assertThat(action.getRetryCount()).isEqualTo(0);
        assertThat(action.getMaxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("approve() transitions PROPOSED to APPROVED")
    void approveHappyPath() {
        var action = createProposedAction();

        action.approve("bob@acme.com");

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.APPROVED);
        assertThat(action.getApprovedBy()).isEqualTo("bob@acme.com");
        assertThat(action.getApprovedAt()).isNotNull();
        assertThat(action.isRequiresApproval()).isFalse();
    }

    @Test
    @DisplayName("reject() transitions PROPOSED to REJECTED")
    void rejectHappyPath() {
        var action = createProposedAction();

        action.reject("bob@acme.com", "Too risky for production");

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.REJECTED);
        assertThat(action.getRejectedBy()).isEqualTo("bob@acme.com");
        assertThat(action.getRejectedAt()).isNotNull();
        assertThat(action.getRejectionReason()).isEqualTo("Too risky for production");
    }

    @Test
    @DisplayName("startExecution() transitions APPROVED to EXECUTING")
    void startExecutionHappyPath() {
        var action = createProposedAction();
        action.approve("bob@acme.com");

        action.startExecution();

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.EXECUTING);
    }

    @Test
    @DisplayName("completeExecution() transitions EXECUTING to COMPLETED")
    void completeExecutionHappyPath() {
        var action = createProposedAction();
        action.approve("bob@acme.com");
        action.startExecution();

        UUID execId = UUID.randomUUID();
        action.completeExecution(execId);

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.COMPLETED);
        assertThat(action.getToolExecutionId()).isEqualTo(execId);
    }

    @Test
    @DisplayName("failExecution() transitions EXECUTING to FAILED and increments retryCount")
    void failExecutionHappyPath() {
        var action = createProposedAction();
        action.approve("bob@acme.com");
        action.startExecution();

        UUID execId = UUID.randomUUID();
        action.failExecution("Connection timeout", execId);

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.FAILED);
        assertThat(action.getLastError()).isEqualTo("Connection timeout");
        assertThat(action.getToolExecutionId()).isEqualTo(execId);
        assertThat(action.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("canRetry() returns true when FAILED and retryCount < maxRetries")
    void canRetryTrue() {
        var action = createProposedAction();
        action.approve("bob@acme.com");
        action.startExecution();
        action.failExecution("error", null);

        assertThat(action.canRetry()).isTrue();
    }

    @Test
    @DisplayName("canRetry() returns false when max retries exhausted")
    void canRetryFalseMaxRetries() {
        var action = createProposedAction();
        action.approve("bob@acme.com");

        // Exhaust all retries
        for (int i = 0; i < 3; i++) {
            action.startExecution();
            action.failExecution("error " + i, null);
        }

        assertThat(action.getRetryCount()).isEqualTo(3);
        assertThat(action.canRetry()).isFalse();
    }

    @Test
    @DisplayName("PROPOSED → EXECUTING is invalid (must approve first)")
    void proposedToExecutingInvalid() {
        var action = createProposedAction();

        assertThatThrownBy(action::startExecution)
            .isInstanceOf(Action.InvalidActionTransitionException.class)
            .hasMessageContaining("PROPOSED")
            .hasMessageContaining("EXECUTING");
    }

    @Test
    @DisplayName("REJECTED → APPROVED is invalid")
    void rejectedToApprovedInvalid() {
        var action = createProposedAction();
        action.reject("bob@acme.com", "no");

        assertThatThrownBy(() -> action.approve("charlie@acme.com"))
            .isInstanceOf(Action.InvalidActionTransitionException.class)
            .hasMessageContaining("REJECTED")
            .hasMessageContaining("APPROVED");
    }

    @Test
    @DisplayName("COMPLETED → EXECUTING is invalid")
    void completedToExecutingInvalid() {
        var action = createProposedAction();
        action.approve("bob@acme.com");
        action.startExecution();
        action.completeExecution(UUID.randomUUID());

        assertThatThrownBy(action::startExecution)
            .isInstanceOf(Action.InvalidActionTransitionException.class)
            .hasMessageContaining("COMPLETED")
            .hasMessageContaining("EXECUTING");
    }

    @Test
    @DisplayName("FAILED → EXECUTING is valid (retry)")
    void failedToExecutingRetry() {
        var action = createProposedAction();
        action.approve("bob@acme.com");
        action.startExecution();
        action.failExecution("transient error", null);

        action.startExecution();

        assertThat(action.getStatus()).isEqualTo(Action.ActionStatus.EXECUTING);
    }
}

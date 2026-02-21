package com.magiconcall.application.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.tool.ExecuteToolCommand;
import com.magiconcall.application.tool.ToolExecutionResult;
import com.magiconcall.application.tool.ToolExecutor;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.policy.ActionPolicyEvaluator;
import com.magiconcall.domain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock private ActionRepository actionRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentEventRepository incidentEventRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ToolExecutor toolExecutionService;
    @Mock private ActionMetrics actionMetrics;

    private ActionService service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID incidentId = UUID.randomUUID();
    private Incident testIncident;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        var evaluator = new ActionPolicyEvaluator("logs,metrics,deploy,topology", 10, true);
        service = new ActionService(
            actionRepository, incidentRepository, incidentEventRepository,
            auditLogRepository, evaluator, toolExecutionService, actionMetrics, objectMapper
        );

        testIncident = new Incident("ext-1", "Test Incident", "summary",
            IncidentSeverity.SEV2, "commander", "{}");
        testIncident.setId(incidentId);
        testIncident.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void stubIncidentFound() {
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(testIncident));
    }

    private void stubActionSave() {
        when(actionRepository.save(any(Action.class))).thenAnswer(invocation -> {
            Action a = invocation.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
    }

    @Test
    @DisplayName("propose READ action is auto-approved")
    void proposeReadAutoApproved() {
        stubIncidentFound();
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var command = new ProposeActionCommand(
            "Check logs", null, "logs", Map.of(), "READ", "alice@acme.com", null
        );

        var result = service.propose(incidentId, command);

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.riskLevel()).isEqualTo("READ");
        assertThat(result.policyAppliedRules()).contains("read_auto_approve_rule");
        verify(actionMetrics).recordProposal("logs", "READ", "auto_approved");
    }

    @Test
    @DisplayName("propose DANGEROUS action is rejected by policy")
    void proposeDangerousRejected() {
        stubIncidentFound();
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var command = new ProposeActionCommand(
            "Drop database", null, "deploy", Map.of(), "DANGEROUS", "alice@acme.com", null
        );

        var result = service.propose(incidentId, command);

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.policyAppliedRules()).contains("dangerous_block_rule");
        verify(actionMetrics).recordProposal("deploy", "DANGEROUS", "rejected");
    }

    @Test
    @DisplayName("propose with existing idempotency key returns existing action")
    void proposeIdempotencyHit() {
        stubIncidentFound();

        var existingAction = Action.propose(
            incidentId, "Existing", null, "logs", "{}", Action.ActionRiskLevel.READ,
            "alice@acme.com", "idem-key-001"
        );
        existingAction.setId(UUID.randomUUID());
        existingAction.setTenantId("tenant-test");

        when(actionRepository.findByIdempotencyKey("idem-key-001"))
            .thenReturn(Optional.of(existingAction));

        var command = new ProposeActionCommand(
            "Check logs", null, "logs", Map.of(), "READ", "alice@acme.com", "idem-key-001"
        );

        var result = service.propose(incidentId, command);

        assertThat(result.id()).isEqualTo(existingAction.getId());
        verify(actionRepository, never()).save(any());
    }

    @Test
    @DisplayName("propose unknown tool is denied")
    void proposeUnknownToolDenied() {
        stubIncidentFound();
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var command = new ProposeActionCommand(
            "Run unknown", null, "unknown-tool", Map.of(), "READ", "alice@acme.com", null
        );

        var result = service.propose(incidentId, command);

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.policyAppliedRules()).contains("tool_allowlist_rule");
    }

    @Test
    @DisplayName("approve transitions PROPOSED to APPROVED")
    void approveHappyPath() {
        var action = Action.propose(
            incidentId, "Scale pods", null, "deploy", "{}", Action.ActionRiskLevel.SAFE_WRITE,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.setRequiresApproval(true);

        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.approve(incidentId, action.getId(), new ApproveActionCommand("bob@acme.com"));

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.approvedBy()).isEqualTo("bob@acme.com");
        verify(actionMetrics).recordApproval("deploy");
    }

    @Test
    @DisplayName("approve wrong status throws InvalidActionTransitionException")
    void approveWrongStatus() {
        var action = Action.propose(
            incidentId, "Scale pods", null, "deploy", "{}", Action.ActionRiskLevel.SAFE_WRITE,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.approve("policy"); // already approved

        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        assertThatThrownBy(() ->
            service.approve(incidentId, action.getId(), new ApproveActionCommand("bob@acme.com")))
            .isInstanceOf(Action.InvalidActionTransitionException.class);
    }

    @Test
    @DisplayName("reject transitions PROPOSED to REJECTED")
    void rejectHappyPath() {
        var action = Action.propose(
            incidentId, "Scale pods", null, "deploy", "{}", Action.ActionRiskLevel.SAFE_WRITE,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");

        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.reject(incidentId, action.getId(), "bob@acme.com", "Not needed");

        assertThat(result.status()).isEqualTo("REJECTED");
        assertThat(result.rejectionReason()).isEqualTo("Not needed");
        verify(actionMetrics).recordRejection("deploy");
    }

    @Test
    @DisplayName("execute approved action completes successfully")
    void executeHappyPath() {
        var action = Action.propose(
            incidentId, "Check logs", null, "logs", "{}", Action.ActionRiskLevel.READ,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.approve("policy");

        UUID execId = UUID.randomUUID();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(toolExecutionService.executeTool(any(ExecuteToolCommand.class)))
            .thenReturn(new ToolExecutionResult(
                execId, "logs", "SUCCESS", "log output", null, 100, 0, null, Instant.now()
            ));

        var result = service.execute(incidentId, action.getId());

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.toolExecutionId()).isEqualTo(execId);
        verify(actionMetrics).recordExecution("logs", "COMPLETED");
    }

    @Test
    @DisplayName("execute failure sets FAILED status")
    void executeFailure() {
        var action = Action.propose(
            incidentId, "Check logs", null, "logs", "{}", Action.ActionRiskLevel.READ,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.approve("policy");

        UUID execId = UUID.randomUUID();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(toolExecutionService.executeTool(any(ExecuteToolCommand.class)))
            .thenReturn(new ToolExecutionResult(
                execId, "logs", "FAILURE", null, "Connection refused", 100, 0, null, Instant.now()
            ));

        var result = service.execute(incidentId, action.getId());

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.lastError()).isEqualTo("Connection refused");
        assertThat(result.retryCount()).isEqualTo(1);
        verify(actionMetrics).recordExecution("logs", "FAILED");
    }

    @Test
    @DisplayName("execute retry from FAILED works")
    void executeRetryFromFailed() {
        var action = Action.propose(
            incidentId, "Check logs", null, "logs", "{}", Action.ActionRiskLevel.READ,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.approve("policy");
        action.startExecution();
        action.failExecution("first error", UUID.randomUUID());

        UUID execId = UUID.randomUUID();
        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));
        stubActionSave();
        when(incidentEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(toolExecutionService.executeTool(any(ExecuteToolCommand.class)))
            .thenReturn(new ToolExecutionResult(
                execId, "logs", "SUCCESS", "retry output", null, 50, 0, null, Instant.now()
            ));

        var result = service.execute(incidentId, action.getId());

        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(actionMetrics).recordRetry("logs");
    }

    @Test
    @DisplayName("execute max retries exceeded throws exception")
    void executeMaxRetriesExceeded() {
        var action = Action.propose(
            incidentId, "Check logs", null, "logs", "{}", Action.ActionRiskLevel.READ,
            "alice@acme.com", null
        );
        action.setId(UUID.randomUUID());
        action.setTenantId("tenant-test");
        action.approve("policy");

        // Exhaust all retries
        for (int i = 0; i < 3; i++) {
            action.startExecution();
            action.failExecution("error " + i, null);
        }

        when(actionRepository.findById(action.getId())).thenReturn(Optional.of(action));

        assertThatThrownBy(() -> service.execute(incidentId, action.getId()))
            .isInstanceOf(Action.InvalidActionTransitionException.class);
    }
}

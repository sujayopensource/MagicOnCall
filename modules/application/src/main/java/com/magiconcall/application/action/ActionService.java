package com.magiconcall.application.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.tool.ExecuteToolCommand;
import com.magiconcall.application.tool.ToolExecutionResult;
import com.magiconcall.application.tool.ToolExecutor;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.policy.ActionPolicyEvaluator;
import com.magiconcall.domain.policy.PolicyContext;
import com.magiconcall.domain.policy.PolicyDecision;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ActionService {

    private static final Logger log = LoggerFactory.getLogger(ActionService.class);

    private final ActionRepository actionRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final ActionPolicyEvaluator policyEvaluator;
    private final ToolExecutor toolExecutionService;
    private final ActionMetrics actionMetrics;
    private final ObjectMapper objectMapper;

    public ActionService(ActionRepository actionRepository,
                         IncidentRepository incidentRepository,
                         IncidentEventRepository incidentEventRepository,
                         AuditLogRepository auditLogRepository,
                         ActionPolicyEvaluator policyEvaluator,
                         ToolExecutor toolExecutionService,
                         ActionMetrics actionMetrics,
                         ObjectMapper objectMapper) {
        this.actionRepository = actionRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.policyEvaluator = policyEvaluator;
        this.toolExecutionService = toolExecutionService;
        this.actionMetrics = actionMetrics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ActionResult propose(UUID incidentId, ProposeActionCommand command) {
        String tenantId = TenantContext.requireTenantId();
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ActionNotFoundException(incidentId, "Incident not found: " + incidentId));

        // Idempotency check
        if (command.idempotencyKey() != null) {
            var existing = actionRepository.findByIdempotencyKey(command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent hit: action with idempotencyKey={} already exists", command.idempotencyKey());
                return ActionResult.from(existing.get());
            }
        }

        var riskLevel = Action.ActionRiskLevel.valueOf(command.riskLevel().toUpperCase());

        // Build policy context
        var policyContext = PolicyContext.builder("propose_action", "action")
            .attribute("riskLevel", command.riskLevel())
            .attribute("toolName", command.toolName())
            .build();

        // Check for scaling parameters
        if (command.parameters() != null && command.parameters().containsKey("scalingTarget")) {
            policyContext = PolicyContext.builder("propose_action", "action")
                .attribute("riskLevel", command.riskLevel())
                .attribute("toolName", command.toolName())
                .attribute("scalingTarget", command.parameters().get("scalingTarget"))
                .build();
        }

        PolicyDecision decision = policyEvaluator.evaluate(policyContext);

        // Create action
        String toolParamsJson = serializeMap(command.parameters());
        var action = Action.propose(
            incidentId, command.title(), command.description(),
            command.toolName(), toolParamsJson, riskLevel,
            command.proposedBy(), command.idempotencyKey()
        );
        action.setTenantId(tenantId);
        action.setPolicyDecisionReason(decision.reason());
        action.setPolicyAppliedRules(String.join(",", decision.appliedRules()));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            switch (decision.outcome()) {
                case DENY -> {
                    // Reject immediately — still create the action for audit trail
                    action.reject("policy", decision.reason());
                    action = actionRepository.save(action);
                    actionMetrics.recordProposal(command.toolName(), command.riskLevel(), "rejected");

                    var timelineEvent = IncidentEvent.actionRejected(
                        incidentId, command.title(), "policy", decision.reason());
                    timelineEvent.setTenantId(tenantId);
                    incidentEventRepository.save(timelineEvent);

                    var audit = AuditLog.created(incidentId, "ACTION", action.getId(),
                        "{\"status\":\"REJECTED\",\"reason\":\"%s\"}".formatted(decision.reason()));
                    audit.setTenantId(tenantId);
                    auditLogRepository.save(audit);

                    log.info("Action rejected by policy: tool={}, reason={}", command.toolName(), decision.reason());
                }
                case ESCALATE -> {
                    // Requires human approval
                    action.setRequiresApproval(true);
                    action = actionRepository.save(action);
                    actionMetrics.recordProposal(command.toolName(), command.riskLevel(), "escalated");

                    var timelineEvent = IncidentEvent.actionProposed(
                        incidentId, command.title(), command.toolName(), command.riskLevel());
                    timelineEvent.setTenantId(tenantId);
                    incidentEventRepository.save(timelineEvent);

                    var audit = AuditLog.created(incidentId, "ACTION", action.getId(),
                        "{\"status\":\"PROPOSED\",\"requiresApproval\":true}");
                    audit.setTenantId(tenantId);
                    auditLogRepository.save(audit);

                    log.info("Action proposed (requires approval): tool={}, risk={}", command.toolName(), command.riskLevel());
                }
                case ALLOW -> {
                    // Auto-approve
                    action.approve("policy");
                    action = actionRepository.save(action);
                    actionMetrics.recordProposal(command.toolName(), command.riskLevel(), "auto_approved");

                    var timelineEvent = IncidentEvent.actionApproved(
                        incidentId, command.title(), "policy");
                    timelineEvent.setTenantId(tenantId);
                    incidentEventRepository.save(timelineEvent);

                    var audit = AuditLog.created(incidentId, "ACTION", action.getId(),
                        "{\"status\":\"APPROVED\",\"autoApproved\":true}");
                    audit.setTenantId(tenantId);
                    auditLogRepository.save(audit);

                    log.info("Action auto-approved: tool={}, risk={}", command.toolName(), command.riskLevel());
                }
            }
        }

        return ActionResult.from(action);
    }

    @Transactional
    public ActionResult approve(UUID incidentId, UUID actionId, ApproveActionCommand command) {
        String tenantId = TenantContext.requireTenantId();
        var action = findActionOrThrow(incidentId, actionId);

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            action.approve(command.approvedBy());
            action = actionRepository.save(action);
            actionMetrics.recordApproval(action.getToolName());

            var timelineEvent = IncidentEvent.actionApproved(
                incidentId, action.getTitle(), command.approvedBy());
            timelineEvent.setTenantId(tenantId);
            incidentEventRepository.save(timelineEvent);

            var audit = AuditLog.statusChanged(incidentId, "ACTION", actionId,
                "{\"status\":\"PROPOSED\"}", "{\"status\":\"APPROVED\",\"approvedBy\":\"%s\"}".formatted(command.approvedBy()));
            audit.setTenantId(tenantId);
            auditLogRepository.save(audit);

            log.info("Action approved: id={}, approvedBy={}", actionId, command.approvedBy());
        }

        return ActionResult.from(action);
    }

    @Transactional
    public ActionResult reject(UUID incidentId, UUID actionId, String rejectedBy, String reason) {
        String tenantId = TenantContext.requireTenantId();
        var action = findActionOrThrow(incidentId, actionId);

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            action.reject(rejectedBy, reason);
            action = actionRepository.save(action);
            actionMetrics.recordRejection(action.getToolName());

            var timelineEvent = IncidentEvent.actionRejected(
                incidentId, action.getTitle(), rejectedBy, reason);
            timelineEvent.setTenantId(tenantId);
            incidentEventRepository.save(timelineEvent);

            var audit = AuditLog.statusChanged(incidentId, "ACTION", actionId,
                "{\"status\":\"PROPOSED\"}", "{\"status\":\"REJECTED\",\"rejectedBy\":\"%s\"}".formatted(rejectedBy));
            audit.setTenantId(tenantId);
            auditLogRepository.save(audit);

            log.info("Action rejected: id={}, rejectedBy={}, reason={}", actionId, rejectedBy, reason);
        }

        return ActionResult.from(action);
    }

    @Transactional
    public ActionResult execute(UUID incidentId, UUID actionId) {
        String tenantId = TenantContext.requireTenantId();
        var action = findActionOrThrow(incidentId, actionId);

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            // Check if this is a retry from FAILED
            boolean isRetry = action.getStatus() == Action.ActionStatus.FAILED;
            if (isRetry && !action.canRetry()) {
                throw new Action.InvalidActionTransitionException(
                    action.getStatus(), Action.ActionStatus.EXECUTING);
            }

            if (isRetry) {
                actionMetrics.recordRetry(action.getToolName());
            }

            action.startExecution();
            action = actionRepository.save(action);

            // Delegate to ToolExecutionService
            var toolCommand = new ExecuteToolCommand(
                action.getToolName(), incidentId,
                deserializeMap(action.getToolParameters()),
                action.getProposedBy()
            );

            try {
                ToolExecutionResult toolResult = toolExecutionService.executeTool(toolCommand);

                if ("SUCCESS".equals(toolResult.status())) {
                    action.completeExecution(toolResult.executionId());
                    action = actionRepository.save(action);
                    actionMetrics.recordExecution(action.getToolName(), "COMPLETED");

                    var timelineEvent = IncidentEvent.actionExecuted(
                        incidentId, action.getTitle(), action.getToolName());
                    timelineEvent.setTenantId(tenantId);
                    incidentEventRepository.save(timelineEvent);

                    var audit = AuditLog.statusChanged(incidentId, "ACTION", actionId,
                        "{\"status\":\"EXECUTING\"}", "{\"status\":\"COMPLETED\"}");
                    audit.setTenantId(tenantId);
                    auditLogRepository.save(audit);

                    log.info("Action executed successfully: id={}, tool={}", actionId, action.getToolName());
                } else {
                    String error = toolResult.errorMessage() != null ? toolResult.errorMessage() : "Tool execution failed";
                    action.failExecution(error, toolResult.executionId());
                    action = actionRepository.save(action);
                    actionMetrics.recordExecution(action.getToolName(), "FAILED");

                    var timelineEvent = IncidentEvent.actionFailed(
                        incidentId, action.getTitle(), error);
                    timelineEvent.setTenantId(tenantId);
                    incidentEventRepository.save(timelineEvent);

                    var audit = AuditLog.statusChanged(incidentId, "ACTION", actionId,
                        "{\"status\":\"EXECUTING\"}", "{\"status\":\"FAILED\",\"error\":\"%s\"}".formatted(error));
                    audit.setTenantId(tenantId);
                    auditLogRepository.save(audit);

                    log.warn("Action execution failed: id={}, tool={}, error={}", actionId, action.getToolName(), error);
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Unexpected execution error";
                action.failExecution(error, null);
                action = actionRepository.save(action);
                actionMetrics.recordExecution(action.getToolName(), "FAILED");

                var timelineEvent = IncidentEvent.actionFailed(
                    incidentId, action.getTitle(), error);
                timelineEvent.setTenantId(tenantId);
                incidentEventRepository.save(timelineEvent);

                log.error("Action execution exception: id={}, tool={}", actionId, action.getToolName(), e);
            }
        }

        return ActionResult.from(action);
    }

    @Transactional(readOnly = true)
    public List<ActionResult> listActions(UUID incidentId) {
        incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ActionNotFoundException(incidentId, "Incident not found: " + incidentId));
        return actionRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
            .stream().map(ActionResult::from).toList();
    }

    @Transactional(readOnly = true)
    public ActionResult findAction(UUID incidentId, UUID actionId) {
        return ActionResult.from(findActionOrThrow(incidentId, actionId));
    }

    private Action findActionOrThrow(UUID incidentId, UUID actionId) {
        return actionRepository.findById(actionId)
            .filter(a -> a.getIncidentId().equals(incidentId))
            .orElseThrow(() -> new ActionNotFoundException(actionId));
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> deserializeMap(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public static class ActionNotFoundException extends RuntimeException {
        public ActionNotFoundException(UUID id) {
            super("Action not found: " + id);
        }

        public ActionNotFoundException(UUID id, String message) {
            super(message);
        }
    }
}

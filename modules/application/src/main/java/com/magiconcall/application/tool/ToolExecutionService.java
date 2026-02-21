package com.magiconcall.application.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.event.DomainEvent;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.tenant.TenantContext;
import com.magiconcall.domain.tool.*;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class ToolExecutionService implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);

    private final ToolRegistry toolRegistry;
    private final ToolRateLimiter rateLimiter;
    private final ToolExecutionLogRepository executionLogRepository;
    private final EvidenceRepository evidenceRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final EventPublisher eventPublisher;
    private final ToolMetrics toolMetrics;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final RetryConfig retryConfig;
    private final TimeLimiterConfig timeLimiterConfig;

    @Autowired
    public ToolExecutionService(ToolRegistry toolRegistry,
                                ToolRateLimiter rateLimiter,
                                ToolExecutionLogRepository executionLogRepository,
                                EvidenceRepository evidenceRepository,
                                IncidentEventRepository incidentEventRepository,
                                EventPublisher eventPublisher,
                                ToolMetrics toolMetrics,
                                ObjectMapper objectMapper) {
        this(toolRegistry, rateLimiter, executionLogRepository, evidenceRepository,
            incidentEventRepository, eventPublisher, toolMetrics, objectMapper,
            RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(RuntimeException.class)
                .build(),
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))
                .cancelRunningFuture(true)
                .build());
    }

    // Package-private constructor for testing with custom configs
    ToolExecutionService(ToolRegistry toolRegistry,
                         ToolRateLimiter rateLimiter,
                         ToolExecutionLogRepository executionLogRepository,
                         EvidenceRepository evidenceRepository,
                         IncidentEventRepository incidentEventRepository,
                         EventPublisher eventPublisher,
                         ToolMetrics toolMetrics,
                         ObjectMapper objectMapper,
                         RetryConfig retryConfig,
                         TimeLimiterConfig timeLimiterConfig) {
        this.toolRegistry = toolRegistry;
        this.rateLimiter = rateLimiter;
        this.executionLogRepository = executionLogRepository;
        this.evidenceRepository = evidenceRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.eventPublisher = eventPublisher;
        this.toolMetrics = toolMetrics;
        this.objectMapper = objectMapper;
        this.retryConfig = retryConfig;
        this.timeLimiterConfig = timeLimiterConfig;
        this.executor = Executors.newCachedThreadPool();
    }

    @Transactional
    public ToolExecutionResult executeTool(ExecuteToolCommand command) {
        String tenantId = TenantContext.requireTenantId();
        String toolName = command.toolName();

        // 1. Rate limit check
        if (!rateLimiter.tryAcquire(toolName, tenantId)) {
            toolMetrics.recordRateLimited(toolName);
            log.info("Tool rate-limited: tool={}, tenant={}", toolName, tenantId);
            return persistAndReturn(command, tenantId, ToolExecutionStatus.RATE_LIMITED,
                null, "Rate limit exceeded for tool: " + toolName, 0, 0);
        }

        // 2. Lookup tool
        Tool tool = toolRegistry.getByName(toolName);
        ToolRequest toolRequest = new ToolRequest(
            command.incidentId(), toolName, command.parameters(), command.requestedBy()
        );

        // 3. Execute with Resilience4j retry + timeout
        Instant start = Instant.now();
        int[] retryCount = {0};
        ToolResponse toolResponse;
        ToolExecutionStatus status;

        try {
            Retry retry = Retry.of("tool-" + toolName, retryConfig);
            retry.getEventPublisher().onRetry(event ->
                retryCount[0] = event.getNumberOfRetryAttempts());

            TimeLimiter timeLimiter = TimeLimiter.of("tool-" + toolName, timeLimiterConfig);

            Callable<ToolResponse> retryWrapped = Retry.decorateCallable(
                retry, () -> tool.execute(toolRequest));

            Future<ToolResponse> future = executor.submit(retryWrapped);
            toolResponse = timeLimiter.executeFutureSupplier(() -> future);
            status = toolResponse.success() ? ToolExecutionStatus.SUCCESS : ToolExecutionStatus.FAILURE;
        } catch (TimeoutException e) {
            toolResponse = ToolResponse.failure("Tool execution timed out");
            status = ToolExecutionStatus.TIMEOUT;
        } catch (Exception e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            toolResponse = ToolResponse.failure(msg);
            status = ToolExecutionStatus.FAILURE;
        }

        long durationMs = Duration.between(start, Instant.now()).toMillis();

        // 4. Persist + store evidence + publish event
        return persistAndReturn(command, tenantId, status, toolResponse,
            toolResponse.errorMessage(), durationMs, retryCount[0]);
    }

    private ToolExecutionResult persistAndReturn(ExecuteToolCommand command, String tenantId,
                                                  ToolExecutionStatus status, ToolResponse toolResponse,
                                                  String errorMessage, long durationMs, int retryCount) {
        String toolName = command.toolName();
        String responseContent = toolResponse != null ? toolResponse.content() : null;

        try (var ignored = MDC.putCloseable("toolName", toolName)) {
            // Persist ToolExecutionLog
            var execLog = new ToolExecutionLog(
                toolName, command.incidentId(), status,
                serializeJson(command.parameters()), responseContent,
                errorMessage, durationMs, retryCount,
                command.requestedBy(), Instant.now()
            );
            execLog.setTenantId(tenantId);
            execLog = executionLogRepository.save(execLog);

            // Record metrics
            toolMetrics.recordExecution(toolName, status.name(), durationMs);

            // Store as Evidence if incidentId provided and execution succeeded
            UUID evidenceId = null;
            if (command.incidentId() != null && status == ToolExecutionStatus.SUCCESS) {
                evidenceId = storeAsEvidence(command, toolResponse, tenantId);
            }

            // Publish domain event
            publishToolExecutedEvent(execLog, tenantId);

            log.info("Tool executed: tool={}, status={}, duration={}ms, retries={}, evidenceId={}",
                toolName, status, durationMs, retryCount, evidenceId);

            return new ToolExecutionResult(
                execLog.getId(), toolName, status.name(),
                responseContent, errorMessage,
                durationMs, retryCount, evidenceId, execLog.getExecutedAt()
            );
        }
    }

    private UUID storeAsEvidence(ExecuteToolCommand command, ToolResponse response, String tenantId) {
        var evidence = new Evidence(
            command.incidentId(), null, EvidenceType.TOOL_OUTPUT,
            "Tool output: " + command.toolName(),
            response.content(), null,
            serializeJson(response.metadata())
        );
        evidence.setTenantId(tenantId);
        evidence = evidenceRepository.save(evidence);

        var timelineEvent = IncidentEvent.evidenceAdded(
            command.incidentId(), "Tool output: " + command.toolName(), "TOOL_OUTPUT");
        timelineEvent.setTenantId(tenantId);
        incidentEventRepository.save(timelineEvent);

        return evidence.getId();
    }

    private void publishToolExecutedEvent(ToolExecutionLog execLog, String tenantId) {
        var payload = Map.of(
            "executionId", execLog.getId().toString(),
            "toolName", execLog.getToolName(),
            "status", execLog.getStatus().name(),
            "durationMs", String.valueOf(execLog.getDurationMs())
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            var domainEvent = DomainEvent.of(
                "TOOL_EXECUTED", "Tool", execLog.getId(), tenantId, json);
            eventPublisher.publish(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tool event", e);
        }
    }

    private String serializeJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

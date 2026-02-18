package com.magiconcall.application.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.tenant.TenantContext;
import com.magiconcall.domain.tool.*;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolExecutionServiceTest {

    @Mock private ToolRateLimiter rateLimiter;
    @Mock private ToolExecutionLogRepository executionLogRepository;
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private IncidentEventRepository incidentEventRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private ToolMetrics toolMetrics;

    private ToolExecutionService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-test");
        when(rateLimiter.tryAcquire(any(), any())).thenReturn(true);

        // Stub repository saves to return argument with an ID set
        when(executionLogRepository.save(any(ToolExecutionLog.class))).thenAnswer(invocation -> {
            ToolExecutionLog log = invocation.getArgument(0);
            if (log.getId() == null) log.setId(UUID.randomUUID());
            return log;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ToolExecutionService buildService(Tool tool, RetryConfig retryConfig,
                                               TimeLimiterConfig timeLimiterConfig) {
        var registry = new ToolRegistry(List.of(tool));
        return new ToolExecutionService(
            registry, rateLimiter, executionLogRepository, evidenceRepository,
            incidentEventRepository, eventPublisher, toolMetrics, objectMapper,
            retryConfig, timeLimiterConfig
        );
    }

    @Test
    @DisplayName("retries on transient failure then succeeds")
    void retryOnTransientFailure() {
        AtomicInteger callCount = new AtomicInteger(0);
        Tool flaky = new Tool() {
            @Override public String name() { return "flaky"; }
            @Override public ToolResponse execute(ToolRequest request) {
                if (callCount.incrementAndGet() <= 2) {
                    throw new RuntimeException("Transient error");
                }
                return ToolResponse.success("recovered", Map.of());
            }
        };

        var retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ZERO)
            .retryExceptions(RuntimeException.class)
            .build();
        var timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        service = buildService(flaky, retryConfig, timeLimiterConfig);

        var result = service.executeTool(new ExecuteToolCommand(
            "flaky", null, Map.of(), "tester"));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.content()).isEqualTo("recovered");
        assertThat(result.retryCount()).isEqualTo(2);
        assertThat(callCount.get()).isEqualTo(3);
        verify(executionLogRepository).save(any(ToolExecutionLog.class));
    }

    @Test
    @DisplayName("all retries exhausted returns FAILURE")
    void allRetriesExhausted() {
        Tool alwaysFails = new Tool() {
            @Override public String name() { return "broken"; }
            @Override public ToolResponse execute(ToolRequest request) {
                throw new RuntimeException("Permanent error");
            }
        };

        var retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ZERO)
            .retryExceptions(RuntimeException.class)
            .build();
        var timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();

        service = buildService(alwaysFails, retryConfig, timeLimiterConfig);

        var result = service.executeTool(new ExecuteToolCommand(
            "broken", null, Map.of(), "tester"));

        assertThat(result.status()).isEqualTo("FAILURE");
        assertThat(result.errorMessage()).contains("Permanent error");
        verify(toolMetrics).recordExecution(eq("broken"), eq("FAILURE"), anyLong());
    }

    @Test
    @DisplayName("timeout produces TIMEOUT status")
    void timeoutProducesTimeoutStatus() {
        Tool slowTool = new Tool() {
            @Override public String name() { return "slow"; }
            @Override public ToolResponse execute(ToolRequest request) {
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return ToolResponse.success("too late", Map.of());
            }
        };

        var retryConfig = RetryConfig.custom()
            .maxAttempts(1)
            .waitDuration(Duration.ZERO)
            .build();
        var timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(100))
            .cancelRunningFuture(true)
            .build();

        service = buildService(slowTool, retryConfig, timeLimiterConfig);

        var result = service.executeTool(new ExecuteToolCommand(
            "slow", null, Map.of(), "tester"));

        assertThat(result.status()).isEqualTo("TIMEOUT");
        assertThat(result.errorMessage()).contains("timed out");
        verify(toolMetrics).recordExecution(eq("slow"), eq("TIMEOUT"), anyLong());
    }

    @Test
    @DisplayName("rate limited returns RATE_LIMITED without calling tool")
    void rateLimitedSkipsTool() {
        when(rateLimiter.tryAcquire(any(), any())).thenReturn(false);

        Tool neverCalled = mock(Tool.class);
        when(neverCalled.name()).thenReturn("limited");

        var retryConfig = RetryConfig.ofDefaults();
        var timeLimiterConfig = TimeLimiterConfig.ofDefaults();

        service = buildService(neverCalled, retryConfig, timeLimiterConfig);

        var result = service.executeTool(new ExecuteToolCommand(
            "limited", null, Map.of(), "tester"));

        assertThat(result.status()).isEqualTo("RATE_LIMITED");
        verify(neverCalled, never()).execute(any());
        verify(toolMetrics).recordRateLimited("limited");
    }

    @Test
    @DisplayName("successful execution with incidentId stores Evidence")
    void successStoresEvidence() {
        Tool goodTool = new Tool() {
            @Override public String name() { return "logs"; }
            @Override public ToolResponse execute(ToolRequest request) {
                return ToolResponse.success("log output", Map.of("key", "value"));
            }
        };

        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> {
            Evidence e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(UUID.randomUUID());
            return e;
        });

        var retryConfig = RetryConfig.custom().maxAttempts(1).build();
        var timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5)).build();

        service = buildService(goodTool, retryConfig, timeLimiterConfig);

        UUID incidentId = UUID.randomUUID();
        var result = service.executeTool(new ExecuteToolCommand(
            "logs", incidentId, Map.of("service", "api"), "tester"));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.evidenceId()).isNotNull();
        verify(evidenceRepository).save(any(Evidence.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
    }

    @Test
    @DisplayName("successful execution without incidentId does not store Evidence")
    void successWithoutIncidentIdSkipsEvidence() {
        Tool goodTool = new Tool() {
            @Override public String name() { return "metrics"; }
            @Override public ToolResponse execute(ToolRequest request) {
                return ToolResponse.success("metric output", Map.of());
            }
        };

        var retryConfig = RetryConfig.custom().maxAttempts(1).build();
        var timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5)).build();

        service = buildService(goodTool, retryConfig, timeLimiterConfig);

        var result = service.executeTool(new ExecuteToolCommand(
            "metrics", null, Map.of(), "tester"));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.evidenceId()).isNull();
        verify(evidenceRepository, never()).save(any());
    }
}

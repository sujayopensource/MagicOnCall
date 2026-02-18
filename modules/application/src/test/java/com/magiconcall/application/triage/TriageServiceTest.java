package com.magiconcall.application.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.incident.IncidentService;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.llm.LlmClient;
import com.magiconcall.domain.llm.LlmRequest;
import com.magiconcall.domain.llm.LlmResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.magiconcall.domain.tenant.TenantContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TriageServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentEventRepository incidentEventRepository;
    @Mock private HypothesisRepository hypothesisRepository;
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private LlmClient llmClient;
    @Mock private TriageMetrics triageMetrics;

    private IncidentService incidentService;
    private EvidenceSummarizer evidenceSummarizer;
    private TriageService triageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID INCIDENT_ID = UUID.randomUUID();
    private static final String TENANT = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);

        incidentService = new IncidentService(
            incidentRepository, incidentEventRepository, hypothesisRepository,
            evidenceRepository, auditLogRepository, eventPublisher, objectMapper
        );
        evidenceSummarizer = new EvidenceSummarizer(evidenceRepository);
        triageService = new TriageService(
            incidentRepository, incidentEventRepository, incidentService,
            evidenceSummarizer, llmClient, triageMetrics, objectMapper, 4096
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("happy path — summarize → LLM → persist → timeline → metrics")
    void happyPath() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        // Evidence for summarizer
        var evidence = new Evidence(INCIDENT_ID, null, EvidenceType.LOG, "Error", "NPE stacktrace", null, "{}");
        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(evidence));

        var llmJson = """
            [{"title":"DB issue","description":"Connection pool exhausted","confidence":0.8,
              "evidenceFor":"Timeout logs","evidenceAgainst":"No recent deploys",
              "nextBestTest":"Check pool metrics","stopCondition":"Pool stable"}]
            """;
        when(llmClient.complete(any(LlmRequest.class)))
            .thenReturn(new LlmResponse(llmJson, 100, 50, "mock", "stop"));

        // Stub hypothesis save to return with ID
        when(hypothesisRepository.save(any(Hypothesis.class))).thenAnswer(inv -> {
            Hypothesis h = inv.getArgument(0);
            if (h.getId() == null) h.setId(UUID.randomUUID());
            return h;
        });
        when(incidentEventRepository.save(any(IncidentEvent.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var result = triageService.triage(INCIDENT_ID);

        assertThat(result.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(result.hypotheses()).hasSize(1);
        assertThat(result.hypotheses().getFirst().title()).isEqualTo("DB issue");
        assertThat(result.hypotheses().getFirst().source()).isEqualTo("AI");
        assertThat(result.hypotheses().getFirst().evidenceFor()).isEqualTo("Timeout logs");
        assertThat(result.evidenceHash()).isNotBlank();
        assertThat(result.cached()).isFalse();
        assertThat(result.tokensUsed()).isEqualTo(150);

        verify(hypothesisRepository).save(any(Hypothesis.class));
        // 2 timeline events: 1 from addHypothesis + 1 from triageCompleted
        verify(incidentEventRepository, times(2)).save(any(IncidentEvent.class));
        verify(triageMetrics).recordTriageRun(false);
        verify(triageMetrics).recordTokensUsed(150);
    }

    @Test
    @DisplayName("cache hit returns cached result without LLM call")
    void cacheHit() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        var evidence = new Evidence(INCIDENT_ID, null, EvidenceType.LOG, "Error", "NPE", null, "{}");
        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(evidence));

        var llmJson = """
            [{"title":"Cache test","description":"desc","confidence":0.5,
              "evidenceFor":"ef","evidenceAgainst":"ea",
              "nextBestTest":"nbt","stopCondition":"sc"}]
            """;
        when(llmClient.complete(any(LlmRequest.class)))
            .thenReturn(new LlmResponse(llmJson, 50, 25, "mock", "stop"));

        when(hypothesisRepository.save(any(Hypothesis.class))).thenAnswer(inv -> {
            Hypothesis h = inv.getArgument(0);
            if (h.getId() == null) h.setId(UUID.randomUUID());
            return h;
        });
        when(incidentEventRepository.save(any(IncidentEvent.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // First call populates cache
        triageService.triage(INCIDENT_ID);

        // Second call should hit cache
        var result = triageService.triage(INCIDENT_ID);

        assertThat(result.cached()).isTrue();
        assertThat(result.tokensUsed()).isEqualTo(0);
        verify(llmClient, times(1)).complete(any());
        verify(triageMetrics).recordTriageRun(true);
    }

    @Test
    @DisplayName("token budget exceeded throws exception")
    void budgetExceeded() {
        // Create service with tiny budget
        triageService = new TriageService(
            incidentRepository, incidentEventRepository, incidentService,
            evidenceSummarizer, llmClient, triageMetrics, objectMapper, 10
        );

        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        var longContent = "x".repeat(1000);
        var evidence = new Evidence(INCIDENT_ID, null, EvidenceType.LOG, "Big", longContent, null, "{}");
        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(evidence));

        assertThatThrownBy(() -> triageService.triage(INCIDENT_ID))
            .isInstanceOf(TokenBudgetExceededException.class)
            .hasMessageContaining("exceeds budget");

        verify(llmClient, never()).complete(any());
        verify(triageMetrics).recordBudgetExceeded();
    }

    @Test
    @DisplayName("incident not found throws IncidentNotFoundException")
    void incidentNotFound() {
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> triageService.triage(INCIDENT_ID))
            .isInstanceOf(IncidentService.IncidentNotFoundException.class);
    }

    private Incident createIncident() {
        var incident = new Incident("ext-1", "Test Incident", "summary",
            IncidentSeverity.SEV2, null, "{}");
        incident.setTenantId(TENANT);
        return incident;
    }
}

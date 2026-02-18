package com.magiconcall.application.triage;

import com.magiconcall.domain.incident.Evidence;
import com.magiconcall.domain.incident.EvidenceRepository;
import com.magiconcall.domain.incident.EvidenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceSummarizerTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @InjectMocks
    private EvidenceSummarizer summarizer;

    private static final UUID INCIDENT_ID = UUID.randomUUID();

    @Test
    @DisplayName("empty evidence produces empty summary")
    void emptyEvidence() {
        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of());

        var summary = summarizer.summarize(INCIDENT_ID);

        assertThat(summary.isEmpty()).isTrue();
        assertThat(summary.formattedSummary()).isEqualTo("No evidence collected yet.");
        assertThat(summary.evidenceHash()).isNotBlank();
    }

    @Test
    @DisplayName("multiple items grouped by type")
    void multipleItemsGroupedByType() {
        var log1 = createEvidence(EvidenceType.LOG, "Error log", "NullPointerException in service");
        var metric1 = createEvidence(EvidenceType.METRIC, "CPU spike", "CPU at 95% for 10 min");
        var log2 = createEvidence(EvidenceType.LOG, "Timeout log", "Connection timeout after 30s");

        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(log1, metric1, log2));

        var summary = summarizer.summarize(INCIDENT_ID);

        assertThat(summary.items()).hasSize(3);
        assertThat(summary.formattedSummary()).contains("## LOG");
        assertThat(summary.formattedSummary()).contains("## METRIC");
        assertThat(summary.formattedSummary()).contains("Error log");
        assertThat(summary.formattedSummary()).contains("CPU spike");
    }

    @Test
    @DisplayName("deterministic hash for same evidence")
    void deterministicHash() {
        var evidence = createEvidence(EvidenceType.LOG, "Error", "Some content");

        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(evidence));

        var summary1 = summarizer.summarize(INCIDENT_ID);
        var summary2 = summarizer.summarize(INCIDENT_ID);

        assertThat(summary1.evidenceHash()).isEqualTo(summary2.evidenceHash());
        assertThat(summary1.evidenceHash()).hasSize(64); // SHA-256 hex
    }

    @Test
    @DisplayName("long content is truncated")
    void truncation() {
        var longContent = "x".repeat(3000);
        var evidence = createEvidence(EvidenceType.LOG, "Big log", longContent);

        when(evidenceRepository.findByIncidentIdOrderByCreatedAtDesc(INCIDENT_ID))
            .thenReturn(List.of(evidence));

        var summary = summarizer.summarize(INCIDENT_ID);

        assertThat(summary.items().getFirst().content()).contains("... [truncated]");
        assertThat(summary.items().getFirst().content().length()).isLessThan(longContent.length());
    }

    private Evidence createEvidence(EvidenceType type, String title, String content) {
        return new Evidence(INCIDENT_ID, null, type, title, content, null, "{}");
    }
}

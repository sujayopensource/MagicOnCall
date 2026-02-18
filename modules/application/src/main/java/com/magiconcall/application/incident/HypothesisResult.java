package com.magiconcall.application.incident;

import com.magiconcall.domain.incident.Hypothesis;

import java.time.Instant;
import java.util.UUID;

public record HypothesisResult(
    UUID id,
    UUID incidentId,
    String title,
    String description,
    String status,
    double confidence,
    String source,
    String evidenceFor,
    String evidenceAgainst,
    String nextBestTest,
    String stopCondition,
    Instant createdAt
) {
    public static HypothesisResult from(Hypothesis h) {
        return new HypothesisResult(
            h.getId(), h.getIncidentId(), h.getTitle(), h.getDescription(),
            h.getStatus().name(), h.getConfidence(), h.getSource(),
            h.getEvidenceFor(), h.getEvidenceAgainst(),
            h.getNextBestTest(), h.getStopCondition(),
            h.getCreatedAt()
        );
    }
}

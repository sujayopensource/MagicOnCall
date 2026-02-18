package com.magiconcall.application.incident;

import com.magiconcall.domain.incident.Evidence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResult(
    UUID id,
    UUID incidentId,
    UUID hypothesisId,
    String evidenceType,
    String title,
    String content,
    String sourceUrl,
    Instant createdAt
) {
    public static EvidenceResult from(Evidence e) {
        return new EvidenceResult(
            e.getId(), e.getIncidentId(), e.getHypothesisId(),
            e.getEvidenceType().name(), e.getTitle(), e.getContent(),
            e.getSourceUrl(), e.getCreatedAt()
        );
    }
}

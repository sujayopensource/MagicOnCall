package com.magiconcall.api.incident;

import com.magiconcall.application.incident.EvidenceResult;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
    UUID id,
    UUID incidentId,
    UUID hypothesisId,
    String evidenceType,
    String title,
    String content,
    String sourceUrl,
    Instant createdAt
) {
    public static EvidenceResponse from(EvidenceResult r) {
        return new EvidenceResponse(
            r.id(), r.incidentId(), r.hypothesisId(), r.evidenceType(),
            r.title(), r.content(), r.sourceUrl(), r.createdAt()
        );
    }
}

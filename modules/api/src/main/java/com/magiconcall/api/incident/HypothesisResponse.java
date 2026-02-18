package com.magiconcall.api.incident;

import com.magiconcall.application.incident.HypothesisResult;

import java.time.Instant;
import java.util.UUID;

public record HypothesisResponse(
    UUID id,
    UUID incidentId,
    String title,
    String description,
    String status,
    double confidence,
    String source,
    Instant createdAt
) {
    public static HypothesisResponse from(HypothesisResult r) {
        return new HypothesisResponse(
            r.id(), r.incidentId(), r.title(), r.description(),
            r.status(), r.confidence(), r.source(), r.createdAt()
        );
    }
}

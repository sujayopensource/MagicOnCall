package com.magiconcall.api.incident;

import com.magiconcall.application.triage.TriageResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TriageResponse(
    UUID incidentId,
    List<HypothesisResponse> hypotheses,
    String evidenceHash,
    int tokensUsed,
    boolean cached,
    Instant timestamp
) {
    public static TriageResponse from(TriageResult r) {
        var hypotheses = r.hypotheses().stream()
            .map(HypothesisResponse::from)
            .toList();
        return new TriageResponse(
            r.incidentId(), hypotheses, r.evidenceHash(),
            r.tokensUsed(), r.cached(), r.timestamp()
        );
    }
}

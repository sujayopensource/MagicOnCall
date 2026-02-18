package com.magiconcall.application.triage;

import com.magiconcall.application.incident.HypothesisResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TriageResult(
    UUID incidentId,
    List<HypothesisResult> hypotheses,
    String evidenceHash,
    int tokensUsed,
    boolean cached,
    Instant timestamp
) {}

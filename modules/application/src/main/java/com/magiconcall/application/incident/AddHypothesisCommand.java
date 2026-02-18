package com.magiconcall.application.incident;

public record AddHypothesisCommand(
    String title,
    String description,
    double confidence,
    String source,
    String evidenceFor,
    String evidenceAgainst,
    String nextBestTest,
    String stopCondition
) {
    /** Backward-compatible constructor for manual (non-AI) hypotheses. */
    public AddHypothesisCommand(String title, String description,
                                double confidence, String source) {
        this(title, description, confidence, source, null, null, null, null);
    }
}

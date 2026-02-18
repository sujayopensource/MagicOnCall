package com.magiconcall.application.triage;

import java.util.List;

public record EvidenceSummary(
    List<EvidenceItem> items,
    String formattedSummary,
    String evidenceHash
) {
    public record EvidenceItem(
        String type,
        String title,
        String content
    ) {}

    public boolean isEmpty() {
        return items.isEmpty();
    }
}

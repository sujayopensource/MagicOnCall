package com.magiconcall.application.incident;

import java.util.UUID;

public record AddEvidenceCommand(
    UUID hypothesisId,
    String evidenceType,
    String title,
    String content,
    String sourceUrl
) {}

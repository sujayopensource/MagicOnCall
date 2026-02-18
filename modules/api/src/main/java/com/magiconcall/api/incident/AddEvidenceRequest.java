package com.magiconcall.api.incident;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AddEvidenceRequest(
    UUID hypothesisId,
    @NotBlank String evidenceType,
    @NotBlank String title,
    String content,
    String sourceUrl
) {}

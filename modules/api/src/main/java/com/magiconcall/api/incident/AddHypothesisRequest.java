package com.magiconcall.api.incident;

import jakarta.validation.constraints.NotBlank;

public record AddHypothesisRequest(
    @NotBlank String title,
    String description,
    double confidence,
    String source
) {}

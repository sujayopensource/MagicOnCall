package com.magiconcall.api.action;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ProposeActionRequest(
    @NotBlank String title,
    String description,
    @NotBlank String toolName,
    Map<String, String> parameters,
    @NotBlank String riskLevel,
    @NotBlank String proposedBy,
    String idempotencyKey
) {}

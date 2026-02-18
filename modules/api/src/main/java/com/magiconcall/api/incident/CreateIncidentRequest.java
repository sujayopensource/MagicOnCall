package com.magiconcall.api.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateIncidentRequest(
    @NotBlank String externalId,
    @NotBlank String title,
    String summary,
    @NotNull String severity,
    String commanderName,
    Map<String, String> tags
) {}

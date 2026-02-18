package com.magiconcall.api.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AlertRequest(
    @NotBlank String externalId,
    @NotBlank String title,
    String description,
    @NotBlank String source,
    @NotNull String severity,
    Map<String, String> labels
) {}

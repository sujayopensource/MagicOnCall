package com.magiconcall.api.graph;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record AddNodeRequest(
    @NotBlank String nodeType,
    @NotBlank String label,
    String description,
    UUID referenceId,
    String source,
    Map<String, String> metadata
) {}

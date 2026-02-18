package com.magiconcall.api.tool;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record ToolRunRequest(
    UUID incidentId,
    Map<String, String> parameters,
    @NotBlank String requestedBy
) {}

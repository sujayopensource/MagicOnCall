package com.magiconcall.api.graph;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record AddEdgeRequest(
    @NotNull UUID sourceNodeId,
    @NotNull UUID targetNodeId,
    @NotBlank String edgeType,
    double weight,
    String reason,
    Map<String, String> metadata
) {}

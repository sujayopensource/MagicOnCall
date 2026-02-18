package com.magiconcall.application.graph;

import java.util.Map;
import java.util.UUID;

public record AddEdgeCommand(
    UUID sourceNodeId,
    UUID targetNodeId,
    String edgeType,
    double weight,
    String reason,
    Map<String, String> metadata
) {}

package com.magiconcall.application.graph;

import java.util.List;
import java.util.UUID;

public record RootCausePath(
    List<UUID> nodeIds,
    List<String> nodeLabels,
    double score,
    String explanation
) {}

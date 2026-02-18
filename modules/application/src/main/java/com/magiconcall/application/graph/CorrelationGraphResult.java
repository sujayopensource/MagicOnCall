package com.magiconcall.application.graph;

import java.util.List;

public record CorrelationGraphResult(
    List<GraphNodeResult> nodes,
    List<GraphEdgeResult> edges
) {}

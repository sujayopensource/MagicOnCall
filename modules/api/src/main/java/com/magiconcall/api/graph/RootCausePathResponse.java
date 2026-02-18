package com.magiconcall.api.graph;

import com.magiconcall.application.graph.RootCausePath;

import java.util.List;
import java.util.UUID;

public record RootCausePathResponse(
    List<UUID> nodeIds,
    List<String> nodeLabels,
    double score,
    String explanation
) {
    public static RootCausePathResponse from(RootCausePath path) {
        return new RootCausePathResponse(
            path.nodeIds(), path.nodeLabels(), path.score(), path.explanation()
        );
    }
}

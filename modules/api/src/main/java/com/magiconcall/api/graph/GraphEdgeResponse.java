package com.magiconcall.api.graph;

import com.magiconcall.application.graph.GraphEdgeResult;

import java.time.Instant;
import java.util.UUID;

public record GraphEdgeResponse(
    UUID id,
    UUID incidentId,
    UUID sourceNodeId,
    UUID targetNodeId,
    String edgeType,
    double weight,
    String reason,
    String metadata,
    Instant createdAt
) {
    public static GraphEdgeResponse from(GraphEdgeResult result) {
        return new GraphEdgeResponse(
            result.id(), result.incidentId(), result.sourceNodeId(),
            result.targetNodeId(), result.edgeType(), result.weight(),
            result.reason(), result.metadata(), result.createdAt()
        );
    }
}

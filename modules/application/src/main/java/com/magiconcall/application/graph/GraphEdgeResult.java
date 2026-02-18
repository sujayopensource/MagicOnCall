package com.magiconcall.application.graph;

import com.magiconcall.domain.graph.CorrelationEdge;

import java.time.Instant;
import java.util.UUID;

public record GraphEdgeResult(
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
    public static GraphEdgeResult from(CorrelationEdge edge) {
        return new GraphEdgeResult(
            edge.getId(), edge.getIncidentId(), edge.getSourceNodeId(),
            edge.getTargetNodeId(), edge.getEdgeType().name(),
            edge.getWeight(), edge.getReason(), edge.getMetadata(),
            edge.getCreatedAt()
        );
    }
}

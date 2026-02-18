package com.magiconcall.api.graph;

import com.magiconcall.application.graph.GraphNodeResult;

import java.time.Instant;
import java.util.UUID;

public record GraphNodeResponse(
    UUID id,
    UUID incidentId,
    String nodeType,
    String label,
    String description,
    UUID referenceId,
    String source,
    String metadata,
    Instant createdAt
) {
    public static GraphNodeResponse from(GraphNodeResult result) {
        return new GraphNodeResponse(
            result.id(), result.incidentId(), result.nodeType(),
            result.label(), result.description(), result.referenceId(),
            result.source(), result.metadata(), result.createdAt()
        );
    }
}

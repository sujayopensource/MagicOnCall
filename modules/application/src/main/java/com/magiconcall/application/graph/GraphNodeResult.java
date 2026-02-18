package com.magiconcall.application.graph;

import com.magiconcall.domain.graph.CorrelationNode;

import java.time.Instant;
import java.util.UUID;

public record GraphNodeResult(
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
    public static GraphNodeResult from(CorrelationNode node) {
        return new GraphNodeResult(
            node.getId(), node.getIncidentId(), node.getNodeType().name(),
            node.getLabel(), node.getDescription(), node.getReferenceId(),
            node.getSource(), node.getMetadata(), node.getCreatedAt()
        );
    }
}

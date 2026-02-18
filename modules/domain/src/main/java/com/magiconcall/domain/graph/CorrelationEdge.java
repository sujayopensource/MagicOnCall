package com.magiconcall.domain.graph;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "correlation_edges")
public class CorrelationEdge extends BaseEntity {

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private UUID sourceNodeId;

    @Column(nullable = false)
    private UUID targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorrelationEdgeType edgeType;

    @Column(nullable = false)
    private double weight;

    @Column(columnDefinition = "text")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    protected CorrelationEdge() {}

    public CorrelationEdge(UUID incidentId, UUID sourceNodeId, UUID targetNodeId,
                           CorrelationEdgeType edgeType, double weight, String reason, String metadata) {
        this.incidentId = incidentId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.edgeType = edgeType;
        this.weight = weight;
        this.reason = reason;
        this.metadata = metadata;
    }

    public UUID getIncidentId() { return incidentId; }
    public UUID getSourceNodeId() { return sourceNodeId; }
    public UUID getTargetNodeId() { return targetNodeId; }
    public CorrelationEdgeType getEdgeType() { return edgeType; }
    public double getWeight() { return weight; }
    public String getReason() { return reason; }
    public String getMetadata() { return metadata; }
}

package com.magiconcall.domain.graph;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "correlation_nodes")
public class CorrelationNode extends BaseEntity {

    @Column(nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorrelationNodeType nodeType;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String description;

    private UUID referenceId;

    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    protected CorrelationNode() {}

    public CorrelationNode(UUID incidentId, CorrelationNodeType nodeType, String label,
                           String description, UUID referenceId, String source, String metadata) {
        this.incidentId = incidentId;
        this.nodeType = nodeType;
        this.label = label;
        this.description = description;
        this.referenceId = referenceId;
        this.source = source;
        this.metadata = metadata;
    }

    public UUID getIncidentId() { return incidentId; }
    public CorrelationNodeType getNodeType() { return nodeType; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public UUID getReferenceId() { return referenceId; }
    public String getSource() { return source; }
    public String getMetadata() { return metadata; }
}

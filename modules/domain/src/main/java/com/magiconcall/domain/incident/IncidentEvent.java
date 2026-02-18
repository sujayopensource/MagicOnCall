package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Append-only timeline entry for an incident.
 */
@Entity
@Table(name = "incident_events")
public class IncidentEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentEventType eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    protected IncidentEvent() {}

    public IncidentEvent(UUID incidentId, IncidentEventType eventType,
                         String description, String metadata) {
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.description = description;
        this.metadata = metadata;
    }

    public static IncidentEvent created(UUID incidentId, String title, String severity) {
        return new IncidentEvent(incidentId, IncidentEventType.CREATED,
            "Incident created: " + title,
            "{\"severity\":\"" + severity + "\"}");
    }

    public static IncidentEvent statusChanged(UUID incidentId,
                                               IncidentStatus from, IncidentStatus to) {
        return new IncidentEvent(incidentId, IncidentEventType.STATUS_CHANGED,
            "Status changed: %s → %s".formatted(from, to),
            "{\"from\":\"%s\",\"to\":\"%s\"}".formatted(from, to));
    }

    public static IncidentEvent hypothesisAdded(UUID incidentId, String hypothesisTitle) {
        return new IncidentEvent(incidentId, IncidentEventType.HYPOTHESIS_ADDED,
            "Hypothesis added: " + hypothesisTitle, "{}");
    }

    public static IncidentEvent evidenceAdded(UUID incidentId, String evidenceTitle, String type) {
        return new IncidentEvent(incidentId, IncidentEventType.EVIDENCE_ADDED,
            "Evidence added: " + evidenceTitle,
            "{\"type\":\"" + type + "\"}");
    }

    public static IncidentEvent alertCorrelated(UUID incidentId, UUID alertId, String alertTitle) {
        return new IncidentEvent(incidentId, IncidentEventType.ALERT_CORRELATED,
            "Alert correlated: " + alertTitle,
            "{\"alertId\":\"" + alertId + "\"}");
    }

    public static IncidentEvent toolExecuted(UUID incidentId, String toolName, String status) {
        return new IncidentEvent(incidentId, IncidentEventType.TOOL_EXECUTED,
            "Tool executed: " + toolName + " (" + status + ")",
            "{\"tool\":\"" + toolName + "\",\"status\":\"" + status + "\"}");
    }

    public static IncidentEvent triageCompleted(UUID incidentId, int hypothesisCount) {
        return new IncidentEvent(incidentId, IncidentEventType.TRIAGE_COMPLETED,
            "AI triage completed: " + hypothesisCount + " hypotheses generated",
            "{\"hypothesisCount\":" + hypothesisCount + "}");
    }

    public static IncidentEvent graphNodeAdded(UUID incidentId, String label, String nodeType) {
        return new IncidentEvent(incidentId, IncidentEventType.GRAPH_NODE_ADDED,
            "Graph node added: " + label + " (" + nodeType + ")",
            "{\"label\":\"" + label + "\",\"nodeType\":\"" + nodeType + "\"}");
    }

    public static IncidentEvent graphSeeded(UUID incidentId, int nodeCount) {
        return new IncidentEvent(incidentId, IncidentEventType.GRAPH_SEEDED,
            "Correlation graph seeded with " + nodeCount + " alert nodes",
            "{\"nodeCount\":" + nodeCount + "}");
    }

    public UUID getIncidentId() { return incidentId; }
    public IncidentEventType getEventType() { return eventType; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
}

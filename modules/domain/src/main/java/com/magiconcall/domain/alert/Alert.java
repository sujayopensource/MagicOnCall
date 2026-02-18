package com.magiconcall.domain.alert;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String labels;

    @Column(nullable = false)
    private String policyDecisionReason;

    private UUID incidentId;

    protected Alert() {}

    public Alert(String externalId, String title, String description, String source,
                 AlertSeverity severity, String labels, String policyDecisionReason) {
        this.externalId = externalId;
        this.title = title;
        this.description = description;
        this.source = source;
        this.severity = severity;
        this.status = AlertStatus.OPEN;
        this.labels = labels;
        this.policyDecisionReason = policyDecisionReason;
    }

    public void acknowledge() { this.status = AlertStatus.ACKNOWLEDGED; }
    public void resolve() { this.status = AlertStatus.RESOLVED; }
    public void suppress() { this.status = AlertStatus.SUPPRESSED; }

    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public String getLabels() { return labels; }
    public String getPolicyDecisionReason() { return policyDecisionReason; }
    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
}

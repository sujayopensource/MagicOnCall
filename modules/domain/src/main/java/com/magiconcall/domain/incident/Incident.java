package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "incidents")
public class Incident extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    private String commanderName;

    private String slackChannelId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String tags;

    protected Incident() {}

    public Incident(String externalId, String title, String summary,
                    IncidentSeverity severity, String commanderName, String tags) {
        this.externalId = externalId;
        this.title = title;
        this.summary = summary;
        this.severity = severity;
        this.status = IncidentStatus.NEW;
        this.commanderName = commanderName;
        this.tags = tags;
    }

    /**
     * Transitions the incident to a new status.
     * Validates the transition is allowed by the state machine.
     *
     * @return the previous status (for audit logging)
     */
    public IncidentStatus transitionTo(IncidentStatus newStatus) {
        this.status.validateTransitionTo(newStatus);
        IncidentStatus previous = this.status;
        this.status = newStatus;
        return previous;
    }

    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public String getCommanderName() { return commanderName; }
    public void setCommanderName(String commanderName) { this.commanderName = commanderName; }
    public String getSlackChannelId() { return slackChannelId; }
    public void setSlackChannelId(String slackChannelId) { this.slackChannelId = slackChannelId; }
    public String getTags() { return tags; }
}

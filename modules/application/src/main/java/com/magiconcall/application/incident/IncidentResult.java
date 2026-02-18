package com.magiconcall.application.incident;

import com.magiconcall.domain.incident.Incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentResult(
    UUID id,
    String externalId,
    String title,
    String summary,
    String severity,
    String status,
    String commanderName,
    String slackChannelId,
    String tenantId,
    Instant createdAt,
    Instant updatedAt
) {
    public static IncidentResult from(Incident incident) {
        return new IncidentResult(
            incident.getId(),
            incident.getExternalId(),
            incident.getTitle(),
            incident.getSummary(),
            incident.getSeverity().name(),
            incident.getStatus().name(),
            incident.getCommanderName(),
            incident.getSlackChannelId(),
            incident.getTenantId(),
            incident.getCreatedAt(),
            incident.getUpdatedAt()
        );
    }
}

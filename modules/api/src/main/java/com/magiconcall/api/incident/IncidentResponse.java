package com.magiconcall.api.incident;

import com.magiconcall.application.incident.IncidentResult;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
    UUID id,
    String externalId,
    String title,
    String summary,
    String severity,
    String status,
    String commanderName,
    String slackChannelId,
    Instant createdAt,
    Instant updatedAt
) {
    public static IncidentResponse from(IncidentResult r) {
        return new IncidentResponse(
            r.id(), r.externalId(), r.title(), r.summary(),
            r.severity(), r.status(), r.commanderName(),
            r.slackChannelId(), r.createdAt(), r.updatedAt()
        );
    }
}

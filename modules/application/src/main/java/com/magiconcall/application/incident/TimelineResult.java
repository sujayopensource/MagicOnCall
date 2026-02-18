package com.magiconcall.application.incident;

import com.magiconcall.domain.incident.IncidentEvent;

import java.time.Instant;
import java.util.UUID;

public record TimelineResult(
    UUID id,
    String eventType,
    String description,
    String metadata,
    String author,
    Instant occurredAt
) {
    public static TimelineResult from(IncidentEvent event) {
        return new TimelineResult(
            event.getId(),
            event.getEventType().name(),
            event.getDescription(),
            event.getMetadata(),
            event.getCreatedBy(),
            event.getCreatedAt()
        );
    }
}

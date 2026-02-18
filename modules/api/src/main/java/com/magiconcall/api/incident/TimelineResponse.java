package com.magiconcall.api.incident;

import com.magiconcall.application.incident.TimelineResult;

import java.time.Instant;
import java.util.UUID;

public record TimelineResponse(
    UUID id,
    String eventType,
    String description,
    String metadata,
    String author,
    Instant occurredAt
) {
    public static TimelineResponse from(TimelineResult r) {
        return new TimelineResponse(
            r.id(), r.eventType(), r.description(),
            r.metadata(), r.author(), r.occurredAt()
        );
    }
}

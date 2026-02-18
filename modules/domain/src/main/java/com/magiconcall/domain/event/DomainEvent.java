package com.magiconcall.domain.event;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
    UUID eventId,
    String eventType,
    String aggregateType,
    UUID aggregateId,
    String tenantId,
    Instant occurredAt,
    String payload
) {
    public static DomainEvent of(String eventType, String aggregateType,
                                  UUID aggregateId, String tenantId, String payload) {
        return new DomainEvent(
            UUID.randomUUID(),
            eventType,
            aggregateType,
            aggregateId,
            tenantId,
            Instant.now(),
            payload
        );
    }
}

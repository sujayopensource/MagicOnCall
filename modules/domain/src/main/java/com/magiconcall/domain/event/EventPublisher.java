package com.magiconcall.domain.event;

/**
 * Port for publishing domain events.
 * Infrastructure layer provides the adapter (outbox-based, Kafka, etc.).
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}

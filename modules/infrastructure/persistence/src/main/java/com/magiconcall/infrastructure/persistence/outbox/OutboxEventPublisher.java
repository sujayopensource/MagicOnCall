package com.magiconcall.infrastructure.persistence.outbox;

import com.magiconcall.domain.event.DomainEvent;
import com.magiconcall.domain.event.EventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes domain events to the outbox table within the current transaction.
 * Must be called inside an existing @Transactional boundary.
 */
@Component
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxRepository outboxRepository;

    public OutboxEventPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        var outboxEvent = new OutboxEvent(
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.tenantId(),
            event.payload()
        );
        outboxRepository.save(outboxEvent);
    }
}

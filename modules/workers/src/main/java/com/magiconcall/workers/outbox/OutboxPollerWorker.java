package com.magiconcall.workers.outbox;

import com.magiconcall.infrastructure.messaging.kafka.KafkaEventPublisher;
import com.magiconcall.infrastructure.persistence.outbox.OutboxEvent;
import com.magiconcall.infrastructure.persistence.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for unpublished events and relays them to Kafka.
 * Runs on a fixed delay. In production, consider CDC (Debezium) instead.
 */
@Component
public class OutboxPollerWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollerWorker.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outboxRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public OutboxPollerWorker(OutboxRepository outboxRepository,
                              KafkaEventPublisher kafkaEventPublisher) {
        this.outboxRepository = outboxRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(fixedDelayString = "${magiconcall.outbox.poll-interval-ms:1000}")
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) {
            return;
        }

        int count = Math.min(events.size(), BATCH_SIZE);
        log.debug("Outbox poller found {} unpublished events, processing {}", events.size(), count);

        for (int i = 0; i < count; i++) {
            OutboxEvent event = events.get(i);
            try {
                kafkaEventPublisher.publish(
                    event.getAggregateType(),
                    event.getEventType(),
                    event.getAggregateId().toString(),
                    event.getPayload()
                );
                event.markPublished();
                outboxRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event: id={}, type={}",
                    event.getId(), event.getEventType(), ex);
                break;
            }
        }
    }
}

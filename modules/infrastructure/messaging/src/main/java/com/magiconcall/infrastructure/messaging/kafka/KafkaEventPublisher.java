package com.magiconcall.infrastructure.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes events to Kafka topics.
 * Topic naming: moc.{aggregateType}.{eventType} (lowercased).
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String aggregateType, String eventType, String key, String payload) {
        String topic = buildTopic(aggregateType, eventType);

        kafkaTemplate.send(topic, key, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to Kafka: topic={}, key={}", topic, key, ex);
                } else {
                    log.debug("Event published to Kafka: topic={}, key={}, offset={}",
                        topic, key, result.getRecordMetadata().offset());
                }
            });
    }

    private String buildTopic(String aggregateType, String eventType) {
        return "moc." + aggregateType.toLowerCase() + "." + eventType.toLowerCase();
    }
}

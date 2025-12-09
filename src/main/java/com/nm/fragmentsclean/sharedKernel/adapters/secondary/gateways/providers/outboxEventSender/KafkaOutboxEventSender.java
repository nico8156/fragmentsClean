package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOutboxEventSender implements OutboxEventSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxEventSender.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaOutboxEventSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(OutboxEventJpaEntity event) throws Exception {
        String topic = topicFor(event);
        String key = keyFor(event);
        String payload = event.getPayloadJson();

        log.info("KafkaOutboxEventSender sending to topic={} key={} type={}",
                topic, key, event.getEventType());

        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send outbox event {} to Kafka", event.getId(), ex);
                    } else {
                        RecordMetadata meta = result.getRecordMetadata();
                        log.info("Outbox event {} sent to Kafka topic={} partition={} offset={}",
                                event.getId(), meta.topic(), meta.partition(), meta.offset());
                    }
                });
    }

    private String topicFor(OutboxEventJpaEntity event) {
        // tu peux raffiner ici : 1 topic par aggregateType, etc.
        // Exemple simple :
        return switch (event.getAggregateType()) {
            case "Article" -> "articles-events";
            case "Coffee" -> "coffees-events";
            case "AuthUser" -> "auth-users-events";
            case "AppUser" -> "app-users-events";
            default -> "domain-events";
        };
    }

    private String keyFor(OutboxEventJpaEntity event) {
        // souvent on met aggregateId ou streamKey comme clé Kafka
        String streamKey = event.getStreamKey();
        return (streamKey != null && !streamKey.isBlank())
                ? streamKey
                : event.getAggregateId();
    }
}

package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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

        // ✅ IMPORTANT : on attend l'ACK Kafka (sinon le dispatcher va marquer SENT trop tôt)
        var future = kafkaTemplate.send(topic, key, payload);
        var result = future.get(10, TimeUnit.SECONDS);

        RecordMetadata meta = result.getRecordMetadata();
        log.info("Outbox event {} sent to Kafka topic={} partition={} offset={}",
                event.getId(), meta.topic(), meta.partition(), meta.offset());
    }

    private String topicFor(OutboxEventJpaEntity event) {
        return switch (event.getAggregateType()) {
            case "Ticket" -> "ticket-verification-requested";
            case "Article" -> "articles-events";
            case "Coffee" -> "coffees-events";
            case "AuthUser" -> "auth-users-events";
            case "AppUser" -> "app-users-events";
            default -> "domain-events";
        };
    }

    private String keyFor(OutboxEventJpaEntity event) {
        String streamKey = event.getStreamKey();
        return (streamKey != null && !streamKey.isBlank())
                ? streamKey
                : event.getAggregateId();
    }
}

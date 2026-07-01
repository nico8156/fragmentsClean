package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxEventSender implements OutboxEventSender {

	private static final Logger log = LoggerFactory.getLogger(KafkaOutboxEventSender.class);

	private final KafkaTemplate<String, String> kafkaTemplate;

	public KafkaOutboxEventSender(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void send(OutboxEventJpaEntity event) throws Exception {
		List<String> topics = topicsFor(event);
		String key = keyFor(event);
		String payload = event.getPayloadJson();

		for (String topic : topics) {
			log.info("KafkaOutboxEventSender sending to topic={} key={} type={}",
					topic, key, event.getEventType());

			ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

			// ✅ Critical: add eventType header so consumers can route without guessing JSON
			// shape
			if (event.getEventType() != null) {
				record.headers().add("type", event.getEventType().getBytes(StandardCharsets.UTF_8));
			}
			if (event.getAggregateType() != null) {
				record.headers().add("aggregateType",
						event.getAggregateType().getBytes(StandardCharsets.UTF_8));
			}
			if (event.getAggregateId() != null) {
				record.headers().add("aggregateId",
						event.getAggregateId().getBytes(StandardCharsets.UTF_8));
			}
			record.headers().add("outboxId",
					String.valueOf(event.getId()).getBytes(StandardCharsets.UTF_8));

			var future = kafkaTemplate.send(record);
			var result = future.get(10, TimeUnit.SECONDS);

			RecordMetadata meta = result.getRecordMetadata();
			log.info("Outbox event {} sent to Kafka topic={} partition={} offset={}",
					event.getId(), meta.topic(), meta.partition(), meta.offset());
		}
	}

	private List<String> topicsFor(OutboxEventJpaEntity event) {
		// Ticket: eventType-based routing
		if ("Ticket".equals(event.getAggregateType())) {
			String t = event.getEventType();

			// FQCN stored in outbox
			if (t.endsWith("TicketVerifyAcceptedEvent")) {
				// ✅ duplicate to projection stream + worker queue
				return List.of("ticket-events", "ticket-verification-requested");
			}
			if (t.endsWith("TicketVerificationCompletedEvent")) {
				return List.of("ticket-events");
			}
			return List.of("ticket-events");
		}

		// Other aggregates: keep your existing convention
		return switch (event.getAggregateType()) {
			case "Article" -> List.of("articles-events");
			case "Ticket" -> List.of("ticket-events");
			case "Coffee" -> List.of("coffees-events");
			case "AuthUser" -> List.of("auth-users-events");
			case "AppUser" -> List.of("app-users-events");
			default -> List.of("domain-events");
		};
	}

	private String keyFor(OutboxEventJpaEntity event) {
		String streamKey = event.getStreamKey();
		return (streamKey != null && !streamKey.isBlank())
				? streamKey
				: event.getAggregateId();
	}
}

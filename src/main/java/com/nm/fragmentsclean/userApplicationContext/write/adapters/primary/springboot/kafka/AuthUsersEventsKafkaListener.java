package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.messaging.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthUsersEventsKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(AuthUsersEventsKafkaListener.class);

	private final ObjectMapper objectMapper;
	private final AuthUserCreatedEventHandler handler;

	public AuthUsersEventsKafkaListener(ObjectMapper objectMapper,
			AuthUserCreatedEventHandler handler) {
		this.objectMapper = objectMapper;
		this.handler = handler;
	}

	@KafkaListener(topics = { "auth-users-events" }, groupId = "user-application-context")
	public void onMessage(ConsumerRecord<String, String> record) {
		String payload = record.value();

		log.info("Kafka listener received raw record on auth-users-events: key={}, value={}",
				record.key(), payload);

		String typeHeader = readHeaderAsString(record, "type");

		// ✅ 1) Route on header if present
		if (typeHeader != null) {
			if (!typeHeader.endsWith("AuthUserCreatedEvent")) {
				log.debug("Ignoring auth-users-events record: type={} (not handled)", typeHeader);
				return;
			}
		} else {
			// ✅ 2) Fallback (si anciens messages sans header type)
			// On inspecte JSON pour éviter de tenter de désérialiser n’importe quoi
			try {
				JsonNode root = objectMapper.readTree(payload);
				boolean looksLikeCreated = root.has("authUserId")
						&& root.has("provider")
						&& root.has("providerUserId")
						&& root.has("emailVerified"); // champ discriminant

				if (!looksLikeCreated) {
					log.debug("Ignoring record (no type header, payload does not look like AuthUserCreatedEvent)");
					return;
				}
			} catch (Exception e) {
				log.error("Failed to inspect payload JSON, skipping. payload={}", payload, e);
				return;
			}
		}

		// ✅ 3) Deserialize only when we are sure it’s the right event
		try {
			AuthUserCreatedEvent event = objectMapper.readValue(payload, AuthUserCreatedEvent.class);

			log.info("Received AuthUserCreatedEvent from Kafka, authUserId={}, email={}",
					event.authUserId(), event.email());

			handler.handle(event);

		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize AuthUserCreatedEvent from payload={}", payload, e);
		}
	}

	private static String readHeaderAsString(ConsumerRecord<String, String> record, String key) {
		Header h = record.headers().lastHeader(key);
		if (h == null || h.value() == null)
			return null;
		return new String(h.value(), StandardCharsets.UTF_8);
	}
}

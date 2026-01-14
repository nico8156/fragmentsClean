package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CoffeeEventsKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(CoffeeEventsKafkaListener.class);

	private final ObjectMapper objectMapper;
	private final CoffeeCreatedEventHandler createdHandler;

	public CoffeeEventsKafkaListener(ObjectMapper objectMapper,
			CoffeeCreatedEventHandler createdHandler) {
		this.objectMapper = objectMapper;
		this.createdHandler = createdHandler;
	}

	@KafkaListener(topics = { "coffees-events" }, groupId = "coffee-context-read")
	public void onMessage(ConsumerRecord<String, String> record) {
		String payload = record.value();

		try {
			JsonNode root = objectMapper.readTree(payload);

			// Guard ultra simple: ignorer ce qui n'est pas un event coffee
			if (!root.has("coffeeId")) {
				log.debug("[coffee-read] ignore non-coffee event on coffees-events: {}", payload);
				return;
			}

			// Pour l’instant: create uniquement
			CoffeeCreatedEvent evt = objectMapper.treeToValue(root, CoffeeCreatedEvent.class);
			createdHandler.handle(evt);

		} catch (Exception e) {
			log.error("[coffee-read] failed to handle coffees-events payload={}", payload, e);
		}
	}
}

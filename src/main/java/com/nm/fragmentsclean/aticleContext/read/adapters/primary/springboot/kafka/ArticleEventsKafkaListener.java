package com.nm.fragmentsclean.aticleContext.read.adapters.primary.springboot.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ArticleEventsKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(ArticleEventsKafkaListener.class);

	private final ObjectMapper objectMapper;
	private final ArticleCreatedEventHandler createdHandler;

	public ArticleEventsKafkaListener(ObjectMapper objectMapper,
			ArticleCreatedEventHandler createdHandler) {
		this.objectMapper = objectMapper;
		this.createdHandler = createdHandler;
	}

	@KafkaListener(topics = { "articles-events" }, groupId = "article-context-read")
	public void onMessage(ConsumerRecord<String, String> record) {
		String payload = record.value();

		try {
			JsonNode root = objectMapper.readTree(payload);

			// Guard simple: on ne traite que les events article
			if (!root.has("articleId")) {
				log.debug("[article-read] ignore non-article event on articles-events: {}", payload);
				return;
			}

			// Pour l’instant : create only
			ArticleCreatedEvent evt = objectMapper.treeToValue(root, ArticleCreatedEvent.class);
			createdHandler.handle(evt);

		} catch (Exception e) {
			log.error("[article-read] failed to handle articles-events payload={}", payload, e);
		}
	}
}

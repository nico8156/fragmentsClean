package com.nm.fragmentsclean.articleContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;

@EmbeddedKafka(partitions = 1, topics = { "articles-events" })
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"logging.level.org.springframework.kafka=INFO",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:schema.sql"
})
@ActiveProfiles("test")
public class ArticleProjectionPipelineIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ARTICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final String SLUG = "quest-ce-que-le-cafe-de-specialite";
	private static final String LOCALE = "fr-FR";

	@Autowired
	MockMvc mockMvc;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	SpringOutboxEventRepository outboxEventRepository;
	@Autowired
	OutboxEventDispatcher outboxEventDispatcher;

	@Autowired
	KafkaListenerEndpointRegistry registry;

	// Optionnel (comme social/coffee) : si tu veux un test sentinelle
	@Autowired(required = false)
	com.nm.fragmentsclean.aticleContext.read.adapters.primary.springboot.kafka.ArticleEventsKafkaListener articleListener;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM articles_projection");
		outboxEventRepository.deleteAll();
	}

	@Test
	void listener_is_loaded() {
		assertThat(articleListener).isNotNull();
	}

	@Test
	void creating_article_populates_articles_projection_via_outbox_and_kafka() throws Exception {
		var clientAt = "2024-02-14T08:00:00Z";

		mockMvc.perform(
				post("/api/articles")
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "articleId": "%s",
								  "authorId": "%s",
								  "authorName": "Hélène Martin",
								  "slug": "%s",
								  "locale": "%s",
								  "title": "Qu'est-ce que le café de spécialité ?",
								  "intro": "Le café de spécialité désigne une approche où chaque étape vise l’excellence.",
								  "blocksJson": "[{\\"heading\\":\\"Une histoire de terroir\\",\\"paragraph\\":\\"Un café de spécialité ne naît jamais par hasard.\\"}]",
								  "conclusion": "Le café de spécialité n’est pas seulement un niveau de qualité.",
								  "coverUrl": "https://images.unsplash.com/cafe.jpg",
								  "coverWidth": 1600,
								  "coverHeight": 1067,
								  "coverAlt": "Branches de caféier chargées de cerises rouges.",
								  "tags": ["Origines", "Qualité", "Terroir"],
								  "readingTimeMin": 6,
								  "coffeeIds": [],
								  "at": "%s"
								}
								"""
								.formatted(COMMAND_ID, ARTICLE_ID, AUTHOR_ID, SLUG,
										LOCALE, clientAt)))
				.andExpect(status().isAccepted());

		// 1) WAIT outbox (commit + création event)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Article'",
					Integer.class);
			assertThat(outboxCount).isNotNull();
			assertThat(outboxCount).isGreaterThan(0);
		});

		// 2) Dispatch outbox -> Kafka (articles-events)
		outboxEventDispatcher.dispatchPending();

		// 2bis) sanity: containers Kafka running (comme social)
		assertThat(registry.getListenerContainers()).isNotEmpty();
		assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning))
				.isTrue();

		// 3) WAIT projection (async Kafka listener)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM articles_projection WHERE id = ?",
					Integer.class,
					ARTICLE_ID);
			assertThat(count).isEqualTo(1);
		});

		// 4) Assert projection content (types JDBC -> Number)
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT * FROM articles_projection WHERE id = ?",
				ARTICLE_ID);

		assertThat(row.get("id")).isEqualTo(ARTICLE_ID);
		assertThat(row.get("slug")).isEqualTo(SLUG);
		assertThat(row.get("locale")).isEqualTo(LOCALE);
		assertThat(row.get("title")).isEqualTo("Qu'est-ce que le café de spécialité ?");
		assertThat(row.get("intro")).isEqualTo(
				"Le café de spécialité désigne une approche où chaque étape vise l’excellence.");
		assertThat(row.get("author_id")).isEqualTo(AUTHOR_ID);
		assertThat(row.get("author_name")).isEqualTo("Hélène Martin");
		assertThat(((Number) row.get("reading_time_min")).intValue()).isEqualTo(6);

		// status: adapte selon ton schema (PUBLISHED / published)
		// assertThat(row.get("status")).isEqualTo("PUBLISHED");

		// 5) Idempotence: redispatch ne doit pas dupliquer
		outboxEventDispatcher.dispatchPending();

		Integer total = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM articles_projection",
				Integer.class);
		assertThat(total).isEqualTo(1);
	}

	// Optionnel : si tu veux un debug utile quand ça casse
	// (à activer ponctuellement)
	@SuppressWarnings("unused")
	private void debugOutbox() {
		System.out.println(jdbcTemplate.queryForList("""
				    SELECT id, aggregate_type, event_type, status, retry_count
				    FROM outbox_events
				    ORDER BY id DESC
				    LIMIT 10
				"""));
	}
}

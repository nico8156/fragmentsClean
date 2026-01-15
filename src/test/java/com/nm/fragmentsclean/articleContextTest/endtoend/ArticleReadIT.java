package com.nm.fragmentsclean.articleContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ArticleReadIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID ARTICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final String SLUG = "quest-ce-que-le-cafe-de-specialite";
	private static final String LOCALE = "fr-FR";

	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	MockMvc mockMvc;
	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	SpringOutboxEventRepository outboxEventRepository;
	@Autowired
	OutboxEventDispatcher outboxEventDispatcher;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM articles_projection");
		outboxEventRepository.deleteAll();
	}

	@Test
	void creating_article_then_reading_by_slug_and_list() throws Exception {
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

		// attendre outbox
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Article'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		// dispatcher outbox -> Kafka
		outboxEventDispatcher.dispatchPending();

		// attendre projection (sinon read = 404)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM articles_projection WHERE id = ?",
					Integer.class,
					ARTICLE_ID);
			assertThat(count).isEqualTo(1);
		});

		// THEN 1 : GET by slug
		// ⚠️ si ton controller a une route différente, change juste ici.
		var result = mockMvc.perform(
				get("/api/articles/{slug}", SLUG)
						.param("locale", LOCALE))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());

		assertThat(root.path("id").asText()).isEqualTo(ARTICLE_ID.toString());
		assertThat(root.path("slug").asText()).isEqualTo(SLUG);
		assertThat(root.path("locale").asText()).isEqualTo(LOCALE);
		assertThat(root.path("title").asText()).isEqualTo("Qu'est-ce que le café de spécialité ?");
		assertThat(root.path("intro").asText()).isEqualTo(
				"Le café de spécialité désigne une approche où chaque étape vise l’excellence.");
		assertThat(root.path("readingTimeMin").asInt()).isEqualTo(6);

		JsonNode author = root.path("author");
		assertThat(author.path("id").asText()).isEqualTo(AUTHOR_ID.toString());
		assertThat(author.path("name").asText()).isEqualTo("Hélène Martin");

		assertThat(root.path("blocks").isArray()).isTrue();
		assertThat(root.path("blocks").size()).isEqualTo(1);
		assertThat(root.path("blocks").get(0).path("heading").asText())
				.isEqualTo("Une histoire de terroir");

		// THEN 2 : list
		mockMvc.perform(
				get("/api/articles")
						.param("locale", LOCALE)
						.param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(ARTICLE_ID.toString()))
				.andExpect(jsonPath("$.items[0].slug").value(SLUG))
				.andExpect(jsonPath("$.items[0].locale").value(LOCALE));
	}
}

package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedKafka(partitions = 1, topics = { "coffees-events" })
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
public class CoffeeProjectionPipelineIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COFFEE_ID = UUID.fromString("07dae867-1273-4d0f-b1dd-f206b290626b");
	private static final String GOOGLE_PLACE_ID = "ChIJB8tVJh3eDkgRrbxiSh2Jj3c";

	@Autowired
	MockMvc mockMvc;
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	SpringOutboxEventRepository outboxRepo;
	@Autowired
	OutboxEventDispatcher outboxEventDispatcher;
	@Autowired
	KafkaListenerEndpointRegistry registry;

	// Si tu as créé le listener coffee, mets le vrai FQCN ici.
	// Ça te donne le même test "listener_is_loaded" que social.
	@Autowired(required = false)
	com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.kafka.CoffeeEventsKafkaListener coffeeListener;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM coffee_summaries_projection");
		outboxRepo.deleteAll();
	}

	@Test
	void listener_is_loaded() {
		assertThat(coffeeListener).isNotNull();
	}

	@Test
	void create_coffee_dispatches_outbox_event_and_projection_is_inserted() throws Exception {
		var clientAt = "2024-02-14T08:00:00Z";

		mockMvc.perform(
				post("/api/coffees")
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "coffeeId": "%s",
								  "googlePlaceId": "%s",
								  "name": "Columbus Café & Co",
								  "addressLine1": "Centre Commercial Grand Quartier",
								  "city": "Saint-Grégoire",
								  "postalCode": "35760",
								  "country": "FR",
								  "lat": 48.1368282,
								  "lon": -1.6953883,
								  "phoneNumber": "02 99 54 25 82",
								  "website": "https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/",
								  "tags": ["espresso", "chain"],
								  "at": "%s"
								}
								"""
								.formatted(
										COMMAND_ID,
										COFFEE_ID,
										GOOGLE_PLACE_ID,
										clientAt)))
				.andExpect(status().isAccepted());

		// 1) attendre que l'outbox contienne l'event Coffee (commit + éventuel async)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Coffee'",
					Integer.class);
			assertThat(outboxCount).isNotNull();
			assertThat(outboxCount).isGreaterThan(0);
		});

		// 2) dispatch outbox -> Kafka (coffees-events)
		outboxEventDispatcher.dispatchPending();

		// 3) sanity: listeners Kafka démarrés (même check que social)
		assertThat(registry.getListenerContainers()).isNotEmpty();
		for (MessageListenerContainer c : registry.getListenerContainers()) {
			System.out.println("KAFKA container: " + c.getListenerId() + " running=" + c.isRunning());
		}
		assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning))
				.isTrue();

		// 4) attendre l'insertion projection
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM coffee_summaries_projection WHERE id = ?",
					Integer.class,
					COFFEE_ID);
			assertThat(count).isEqualTo(1);
		});

		// 5) assertions de contenu
		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT * FROM coffee_summaries_projection WHERE id = ?",
				COFFEE_ID);

		assertThat(row.get("google_place_id")).isEqualTo(GOOGLE_PLACE_ID);
		assertThat(row.get("name")).isEqualTo("Columbus Café & Co");
		assertThat(row.get("address_line1")).isEqualTo("Centre Commercial Grand Quartier");
		assertThat(row.get("city")).isEqualTo("Saint-Grégoire");
		assertThat(row.get("postal_code")).isEqualTo("35760");
		assertThat(row.get("country")).isEqualTo("FR");

		// attention aux types JDBC (Double/BigDecimal)
		assertThat(((Number) row.get("lat")).doubleValue()).isEqualTo(48.1368282);
		assertThat(((Number) row.get("lon")).doubleValue()).isEqualTo(-1.6953883);

		assertThat(row.get("phone_number")).isEqualTo("02 99 54 25 82");
		assertThat(row.get("website")).isEqualTo(
				"https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");
		assertThat(((Number) row.get("version")).longValue()).isGreaterThanOrEqualTo(0L);

		// 6) Idempotence: dispatcher appelé 2 fois ne duplique pas la projection
		outboxEventDispatcher.dispatchPending();

		Integer total = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM coffee_summaries_projection",
				Integer.class);
		assertThat(total).isEqualTo(1);
	}
}

package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

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

@EmbeddedKafka(partitions = 1, topics = { "auth-users-events", "app-users-events" })
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
public class AuthUserKafkaIntegrationTest extends AbstractBaseE2E {

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

	// Sentinelles (optionnel) : adapte les FQCN si besoin
	@Autowired(required = false)
	com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.kafka.AuthUsersEventsKafkaListener authUsersListener;

	@Autowired(required = false)
	com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka.AppUsersEventsKafkaListener appUsersListener;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM user_social_projection");
		jdbcTemplate.update("DELETE FROM refresh_tokens");
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
		outboxEventRepository.deleteAll();
	}

	@Test
	void listeners_are_loaded() {
		assertThat(authUsersListener).isNotNull();
		assertThat(appUsersListener).isNotNull();
	}

	@Test
	void google_login_pipeline_auth_to_user_to_social_projection() throws Exception {
		// GIVEN
		String authorizationCode = "userA";

		// WHEN : login (DTO attendu: authorizationCode)
		mockMvc.perform(
				post("/auth/google/exchange")
						.contentType("application/json")
						.content("""
								{ "authorizationCode": "%s" }
								""".formatted(authorizationCode)))
				.andExpect(status().isOk());

		// 1) WAIT outbox AuthUser (commit + event)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxAuth = jdbcTemplate.queryForObject("""
					SELECT COUNT(*)
					FROM outbox_events
					WHERE aggregate_type = 'AuthUser'
					""", Integer.class);
			assertThat(outboxAuth).isNotNull();
			assertThat(outboxAuth).isGreaterThan(0);
		});

		// 2) Dispatch outbox -> Kafka (auth-users-events)
		outboxEventDispatcher.dispatchPending();

		// 2bis) sanity Kafka containers running
		assertThat(registry.getListenerContainers()).isNotEmpty();
		assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning))
				.isTrue();

		// 3) WAIT AppUser created by userApplicationContext consumer
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer appUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class);
			assertThat(appUsers).isEqualTo(1);
		});

		// 4) WAIT outbox AppUser (created by userContext after consuming
		// auth-users-events)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxApp = jdbcTemplate.queryForObject("""
					SELECT COUNT(*)
					FROM outbox_events
					WHERE aggregate_type = 'AppUser'
					""", Integer.class);
			assertThat(outboxApp).isNotNull();
			assertThat(outboxApp).isGreaterThan(0);
		});

		// 5) Dispatch outbox again -> Kafka (app-users-events) -> social projection
		outboxEventDispatcher.dispatchPending();

		// 6) WAIT social projection
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_social_projection",
					Integer.class);
			assertThat(count).isEqualTo(1);
		});

		var row = jdbcTemplate.queryForMap("SELECT * FROM user_social_projection LIMIT 1");
		assertThat(row.get("display_name")).isNotNull();
		// avatar_url peut être null (AppUser minimal) => pas d'assert strict
	}
}

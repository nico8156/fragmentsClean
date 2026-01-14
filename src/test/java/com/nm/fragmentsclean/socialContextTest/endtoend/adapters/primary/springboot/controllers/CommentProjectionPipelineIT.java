package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;

import org.springframework.test.context.TestPropertySource;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedKafka(partitions = 1, topics = { "domain-events" })
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
		"spring.kafka.consumer.auto-offset-reset=earliest",
		"logging.level.org.springframework.kafka=INFO",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:schema.sql"
})
public class CommentProjectionPipelineIT extends AbstractBaseE2E {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	SpringOutboxEventRepository outboxRepo;
	@Autowired
	OutboxEventDispatcher outboxEventDispatcher;
	@Autowired
	KafkaListenerEndpointRegistry registry;

	@Autowired(required = false)
	com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka.SocialEventsKafkaListener socialListener;

	private static RequestPostProcessor authUser(UUID userId) {
		return jwt().jwt(j -> j
				.subject(userId.toString())
				.claim("roles", List.of("USER")));
	}

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM social_comments_projection");
		try {
			jdbcTemplate.update("DELETE FROM user_social_projection");
		} catch (Exception ignored) {
		}
		outboxRepo.deleteAll();
	}

	@Test
	void listener_is_loaded() {
		assertThat(socialListener).isNotNull();
	}

	@Test
	void create_comment_dispatches_outbox_event_and_projection_is_inserted() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		UUID commandId = UUID.randomUUID();
		Instant at = Instant.now();

		mockMvc.perform(
				post("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "targetId": "%s",
								  "parentId": null,
								  "body": "hello from it",
								  "at": "%s"
								}
								""".formatted(commandId, commentId, targetId, at)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isNotNull();
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		assertThat(registry.getListenerContainers()).isNotEmpty();
		for (MessageListenerContainer c : registry.getListenerContainers()) {
			System.out.println("KAFKA container: " + c.getListenerId() + " running=" + c.isRunning());
		}
		assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning))
				.isTrue();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
					Integer.class,
					commentId);
			assertThat(count).isEqualTo(1);
		});

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT id, target_id, author_id, body, moderation, version FROM social_comments_projection WHERE id = ?",
				commentId);
		assertThat(row.get("target_id")).isEqualTo(targetId);
		assertThat(row.get("author_id")).isEqualTo(userId);
		assertThat(row.get("body")).isEqualTo("hello from it");
		assertThat(((Number) row.get("version")).longValue()).isGreaterThanOrEqualTo(0L);

		// Idempotence: dispatcher called twice should not duplicate projection rows
		outboxEventDispatcher.dispatchPending();
		Integer total = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM social_comments_projection",
				Integer.class);
		assertThat(total).isEqualTo(1);
	}

	@Test
	void update_comment_projects_body_and_editedAt() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();

		UUID createCommandId = UUID.randomUUID();
		Instant createdAt = Instant.now();

		mockMvc.perform(
				post("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "targetId": "%s",
								  "parentId": null,
								  "body": "initial",
								  "at": "%s"
								}
								""".formatted(createCommandId, commentId, targetId,
								createdAt)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
					Integer.class,
					commentId);
			assertThat(count).isEqualTo(1);
		});

		UUID updateCommandId = UUID.randomUUID();
		Instant editedAt = Instant.now();

		mockMvc.perform(
				put("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "body": "updated body",
								  "editedAt": "%s"
								}
								""".formatted(updateCommandId, commentId, editedAt)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Map<String, Object> row = jdbcTemplate.queryForMap(
					"SELECT body, edited_at FROM social_comments_projection WHERE id = ?",
					commentId);
			assertThat(row.get("body")).isEqualTo("updated body");
			assertThat(row.get("edited_at")).isNotNull();
		});
	}

	@Test
	void delete_comment_dispatches_outbox_event_and_projection_sets_deleted_at() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();

		UUID createCommandId = UUID.randomUUID();
		Instant createdAt = Instant.now();

		mockMvc.perform(
				post("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "targetId": "%s",
								  "parentId": null,
								  "body": "to delete",
								  "at": "%s"
								}
								""".formatted(createCommandId, commentId, targetId,
								createdAt)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
					Integer.class,
					commentId);
			assertThat(count).isEqualTo(1);
		});

		UUID deleteCommandId = UUID.randomUUID();
		Instant deletedAt = Instant.now();

		mockMvc.perform(
				delete("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "deletedAt": "%s"
								}
								""".formatted(deleteCommandId, commentId, deletedAt)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Map<String, Object> row = jdbcTemplate.queryForMap("""
					    SELECT deleted_at, moderation, version
					    FROM social_comments_projection
					    WHERE id = ?
					""", commentId);

			assertThat(row.get("deleted_at")).isNotNull();
			assertThat(row.get("moderation")).isEqualTo("SOFT_DELETED");
			assertThat(((Number) row.get("version")).longValue()).isGreaterThanOrEqualTo(1L);
		});
	}

	@Test
	void list_comments_returns_enriched_author_from_user_social_projection() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		UUID commandId = UUID.randomUUID();
		Instant now = Instant.now();

		jdbcTemplate.update("""
				INSERT INTO user_social_projection (
				  user_id, display_name, avatar_url, created_at, updated_at, version
				)
				VALUES (?,?,?,?,?,?)
				""",
				userId,
				"Nina",
				"https://example.com/nina.png",
				Timestamp.from(now),
				Timestamp.from(now),
				1L);

		mockMvc.perform(
				post("/api/social/comments")
						.with(authUser(userId))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "targetId": "%s",
								  "parentId": null,
								  "body": "hello",
								  "at": "%s"
								}
								""".formatted(commandId, commentId, targetId, now)))
				.andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer outboxCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
					Integer.class);
			assertThat(outboxCount).isGreaterThan(0);
		});

		outboxEventDispatcher.dispatchPending();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
					Integer.class,
					commentId);
			assertThat(count).isEqualTo(1);
			mockMvc.perform(
					get("/api/social/comments")
							.with(authUser(userId)) // garde si endpoint sécurisé
							.param("targetId", targetId.toString())
							.param("op", "retrieve")
							.param("limit", "20"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.targetId").value(targetId.toString()))
					.andExpect(jsonPath("$.items[0].id").value(commentId.toString()))
					.andExpect(jsonPath("$.items[0].authorId").value(userId.toString()))
					.andExpect(jsonPath("$.items[0].authorName").value("Nina"))
					.andExpect(jsonPath("$.items[0].avatarUrl")
							.value("https://example.com/nina.png"));

		});
	}
}

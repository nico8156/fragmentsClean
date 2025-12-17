package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContextTest.e2e.AbstractBaseE2E;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("auth_test")
@EmbeddedKafka(partitions = 1, topics = {"domain-events"})
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

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired SpringOutboxEventRepository outboxRepo;
    @Autowired OutboxEventDispatcher outboxEventDispatcher;
    @Autowired KafkaListenerEndpointRegistry registry;

    @Autowired(required = false)
    com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka.SocialEventsKafkaListener socialListener;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM social_comments_projection");
        outboxRepo.deleteAll();
    }

    @Test
    void listener_is_loaded() {
        assertThat(socialListener).isNotNull();
    }

    @Test
    void create_comment_dispatches_outbox_event_and_projection_is_inserted() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        Instant at = Instant.now();

        // WHEN: call write endpoint with a mocked JWT principal
        mockMvc.perform(
                post("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                            {
                              "commandId": "%s",
                              "commentId": "%s",
                              "userId": "%s",
                              "targetId": "%s",
                              "parentId": null,
                              "body": "hello from it",
                              "at": "%s"
                            }
                            """.formatted(
                                commandId, commentId, userId, targetId, at.toString()
                        ))
        ).andExpect(status().isAccepted());

        // THEN: outbox contains at least one event for Comment aggregate
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isNotNull();
            assertThat(outboxCount).isGreaterThan(0);
        });

        // WHEN: outbox -> kafka
        outboxEventDispatcher.dispatchPending();

        // sanity: listeners running
        for (MessageListenerContainer c : registry.getListenerContainers()) {
            System.out.println("KAFKA container: " + c.getListenerId() + " running=" + c.isRunning());
        }
        assertThat(registry.getListenerContainers()).isNotEmpty();
        assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning)).isTrue();

        // THEN: projection row exists
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
                    Integer.class,
                    commentId
            );
            assertThat(count).isEqualTo(1);
        });

        // Bonus: content check
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT id, target_id, author_id, body, moderation, version FROM social_comments_projection WHERE id = ?",
                commentId
        );

        assertThat(row.get("target_id")).isEqualTo(targetId);
        assertThat(row.get("author_id")).isEqualTo(userId);
        assertThat(row.get("body")).isEqualTo("hello from it");
        assertThat(((Number) row.get("version")).longValue()).isGreaterThanOrEqualTo(0L);

        // Grab payload BEFORE dispatch (sinon il peut être supprimé/flaggué)
        String outboxPayload = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM outbox_events WHERE aggregate_type = 'Comment' ORDER BY id ASC LIMIT 1",
                String.class
        );
        assertThat(outboxPayload).isNotBlank();

        // --- Idempotence / redelivery: resend same event payload to Kafka ---
        kafkaTemplate.send("domain-events", commentId.toString(), outboxPayload);

// Assert no duplicate row created
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
                    Integer.class,
                    commentId
            );
            assertThat(count).isEqualTo(1);
        });

// (bonus) table still has exactly one row
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM social_comments_projection",
                Integer.class
        );
        assertThat(total).isEqualTo(1);

    }

    @Test
    void update_comment_projects_body_and_editedAt() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        // 1) Create
        UUID createCommandId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        mockMvc.perform(
                post("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                        {
                          "commandId": "%s",
                          "commentId": "%s",
                          "userId": "%s",
                          "targetId": "%s",
                          "parentId": null,
                          "body": "initial",
                          "at": "%s"
                        }
                        """.formatted(createCommandId, commentId, userId, targetId, createdAt))
        ).andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);
        });
        outboxEventDispatcher.dispatchPending();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
                    Integer.class,
                    commentId
            );
            assertThat(count).isEqualTo(1);
        });

        // 2) Update
        UUID updateCommandId = UUID.randomUUID();
        Instant editedAt = Instant.now();
        Map<String, Object> before = jdbcTemplate.queryForMap(
                "SELECT body, version FROM social_comments_projection WHERE id = ?",
                commentId
        );
        System.out.println("BEFORE UPDATE projection=" + before);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                        {
                          "commandId": "%s",
                          "commentId": "%s",
                          "body": "updated body",
                          "editedAt": "%s"
                        }
                        """.formatted(updateCommandId, commentId, editedAt))
        ).andExpect(status().isAccepted());

        // attend outbox + dispatch
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);
        });
        var events = jdbcTemplate.queryForList("""
  SELECT event_type, payload_json
  FROM outbox_events
  WHERE aggregate_type = 'Comment'
  ORDER BY id DESC
  LIMIT 5
""");
        System.out.println("OUTBOX Comment events=" + events);

        outboxEventDispatcher.dispatchPending();
        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT body, version, edited_at FROM social_comments_projection WHERE id = ?",
                commentId
        );
        System.out.println("AFTER UPDATE projection=" + after);

        // 3) Assert projection updated
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT body, edited_at FROM social_comments_projection WHERE id = ?",
                    commentId
            );
            assertThat(row.get("body")).isEqualTo("updated body");
            assertThat(row.get("edited_at")).isNotNull();
        });
    }

    @Test
    void delete_comment_dispatches_outbox_event_and_projection_sets_deleted_at() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        // 1) Create
        UUID createCommandId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        mockMvc.perform(
                post("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                        {
                          "commandId": "%s",
                          "commentId": "%s",
                          "userId": "%s",
                          "targetId": "%s",
                          "parentId": null,
                          "body": "to delete",
                          "at": "%s"
                        }
                        """.formatted(createCommandId, commentId, userId, targetId, createdAt))
        ).andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);
        });

        outboxEventDispatcher.dispatchPending();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
                    Integer.class,
                    commentId
            );
            assertThat(count).isEqualTo(1);
        });

        // 2) Delete
        UUID deleteCommandId = UUID.randomUUID();
        Instant deletedAt = Instant.now();

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                        {
                          "commandId": "%s",
                          "commentId": "%s",
                          "deletedAt": "%s"
                        }
                        """.formatted(deleteCommandId, commentId, deletedAt))
        ).andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);
        });

        outboxEventDispatcher.dispatchPending();

        // THEN: projection deleted_at set + moderation soft_deleted + version bumped
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
        // GIVEN
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        Instant now = Instant.now();

        // Seed user_social_projection (no dependency on user pipeline)
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
                1L
        );

        // WHEN: create comment (write)
        mockMvc.perform(
                post("/api/social/comments")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                        {
                          "commandId": "%s",
                          "commentId": "%s",
                          "userId": "%s",
                          "targetId": "%s",
                          "parentId": null,
                          "body": "hello",
                          "at": "%s"
                        }
                        """.formatted(commandId, commentId, userId, targetId, now))
        ).andExpect(status().isAccepted());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Comment'",
                    Integer.class
            );
            assertThat(outboxCount).isNotNull();
            assertThat(outboxCount).isGreaterThan(0);
        });

        outboxEventDispatcher.dispatchPending();

        // Wait projection exists
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM social_comments_projection WHERE id = ?",
                    Integer.class,
                    commentId
            );
            assertThat(count).isEqualTo(1);
        });

        // THEN: read endpoint returns enriched authorName/avatarUrl
        mockMvc.perform(
                        get("/api/social-context/comments")
                                .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))) // keep if endpoint is secured
                                .param("targetId", targetId.toString())
                                .param("op", "retrieve")
                                .param("limit", "20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(targetId.toString()))
                .andExpect(jsonPath("$.items[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.items[0].authorId").value(userId.toString()))
                .andExpect(jsonPath("$.items[0].authorName").value("Nina"))
                .andExpect(jsonPath("$.items[0].avatarUrl").value("https://example.com/nina.png"));
    }



}

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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}

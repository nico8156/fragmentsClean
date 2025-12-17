package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"app-users-events", "auth-users-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
})
public class AppUserKafkaTrackerIT extends AbstractBaseE2E {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired SpringOutboxEventRepository outboxRepo;
    @Autowired OutboxEventDispatcher outboxEventDispatcher;
    @Autowired org.springframework.core.env.Environment env;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM user_social_projection");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
        outboxRepo.deleteAll();
    }

    @Test
    void google_login_dispatches_appuser_event_to_kafka_topic() throws Exception {
        String authorizationCode = "userA";

        // Create probe consumer BEFORE dispatch to avoid races
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(Collections.singletonList("app-users-events"));

            // WHEN: login
            mockMvc.perform(
                            post("/auth/google/exchange")
                                    .contentType("application/json")
                                    .content("""
                                        { "authorizationCode": "%s" }
                                    """.formatted(authorizationCode))
                    )
                    .andExpect(status().isOk());

            // sanity: outbox has AppUser event
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type='AppUser'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);

            // WHEN: outbox -> kafka
            outboxEventDispatcher.dispatchPending();

            // THEN: poll until we see a record
            ConsumerRecord<String, String> found = pollForRecord(consumer, Duration.ofSeconds(5));

            assertThat(found).isNotNull();
            assertThat(found.topic()).isEqualTo("app-users-events");
            var evt = objectMapper.readValue(
                    found.value(),
                    com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent.class
            );

            assertThat(evt.userId()).isNotNull();
            assertThat(evt.authUserId()).isNotNull();
            assertThat(evt.displayName()).isEqualTo("User userA");
            assertThat(evt.avatarUrl()).isEqualTo("https://example.com/avatar/userA");
            assertThat(evt.version()).isEqualTo(0L);
            System.out.println("KAFKA record value = " + found.value());
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        String brokers = env.getProperty("spring.embedded.kafka.brokers");
        assertThat(brokers).as("spring.embedded.kafka.brokers must be set").isNotBlank();

        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);

        props.put("group.id", "probe-" + UUID.randomUUID());
        props.put("enable.auto.commit", "true");
        props.put("auto.offset.reset", "earliest");

        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        return new KafkaConsumer<>(props);
    }

    private ConsumerRecord<String, String> pollForRecord(KafkaConsumer<String, String> consumer, Duration maxWait) {
        long deadline = System.currentTimeMillis() + maxWait.toMillis();
        while (System.currentTimeMillis() < deadline) {
            var records = consumer.poll(Duration.ofMillis(250));
            for (var r : records) {
                if ("app-users-events".equals(r.topic())) {
                    return r;
                }
            }
        }
        return null;
    }
}

package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
        "logging.level.com.nm.fragmentsclean.socialContext.read=INFO",
        "logging.level.org.springframework.kafka=INFO",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
public class UserSocialProjectionPipelineIT extends AbstractBaseE2E {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SpringOutboxEventRepository outboxRepo;
    @Autowired OutboxEventDispatcher outboxEventDispatcher;
    @Autowired ObjectMapper objectMapper;
    @Autowired org.springframework.core.env.Environment env;
    @Autowired KafkaListenerEndpointRegistry registry;


    @Autowired(required = false)
    com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka.AppUsersEventsKafkaListener appUsersListener;

    @Test
    void listener_is_loaded() {
        assertThat(appUsersListener).isNotNull();
    }

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM user_social_projection");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
        outboxRepo.deleteAll();
        Integer exists = jdbcTemplate.queryForObject("""
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = 'public'
    AND table_name = 'user_social_projection'
""", Integer.class);

        System.out.println("user_social_projection exists? " + exists);

    }

    @Test
    void google_login_dispatches_appuser_event_and_social_projection_is_upserted() throws Exception {
        String authorizationCode = "userA";

        // (optionnel) probe consumer pour vérifier le record kafka et récupérer le userId
        try (KafkaConsumer<String, String> probe = createConsumer("probe-" + UUID.randomUUID())) {
            probe.subscribe(Collections.singletonList("app-users-events"));

            // WHEN: login
            mockMvc.perform(
                            post("/auth/google/exchange")
                                    .contentType("application/json")
                                    .content("""
                                        { "authorizationCode": "%s" }
                                    """.formatted(authorizationCode))
                    )
                    .andExpect(status().isOk());

            // outbox contient AppUser
            Integer outboxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type='AppUser'",
                    Integer.class
            );
            assertThat(outboxCount).isGreaterThan(0);

            // WHEN: outbox -> kafka
            outboxEventDispatcher.dispatchPending();

            for (MessageListenerContainer c : registry.getListenerContainers()) {
                System.out.println("KAFKA container: " + c.getListenerId() + " running=" + c.isRunning());
            }
            assertThat(registry.getListenerContainers())
                    .as("KafkaListener containers must be present")
                    .isNotEmpty();

            assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning))
                    .as("At least one KafkaListener container must be running")
                    .isTrue();


            // THEN: on lit le message kafka (preuve)
            var record = pollForValue(probe, Duration.ofSeconds(5));
            assertThat(record).isNotNull();

            AppUserCreatedEvent evt = objectMapper.readValue(record, AppUserCreatedEvent.class);
            UUID userId = evt.userId();

            // THEN: la projection social doit être upsertée
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM user_social_projection WHERE user_id = ?",
                        Integer.class,
                        userId
                );
                assertThat(count).isEqualTo(1);
            });

            // Bonus: vérifier contenu
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT user_id, display_name, avatar_url, version FROM user_social_projection WHERE user_id = ?",
                    userId
            );

            assertThat(row.get("display_name")).isEqualTo("User userA");
            assertThat(row.get("avatar_url")).isEqualTo("https://example.com/avatar/userA");
            assertThat(((Number) row.get("version")).longValue()).isEqualTo(0L);
        }
    }

    private KafkaConsumer<String, String> createConsumer(String groupId) {
        String brokers = env.getProperty("spring.embedded.kafka.brokers");
        assertThat(brokers).isNotBlank();

        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());

        return new KafkaConsumer<>(props);
    }

    private String pollForValue(KafkaConsumer<String, String> consumer, Duration maxWait) {
        long deadline = System.currentTimeMillis() + maxWait.toMillis();
        while (System.currentTimeMillis() < deadline) {
            var records = consumer.poll(Duration.ofMillis(250));
            for (var r : records) {
                if ("app-users-events".equals(r.topic())) {
                    return r.value();
                }
            }
        }
        return null;
    }
}

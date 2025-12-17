package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"

})
public class AuthUserKafkaIntegrationTest extends AbstractBaseE2E {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Autowired
    SpringOutboxEventRepository outboxRepo;
    @Autowired
    OutboxEventDispatcher outboxEventDispatcher;

    @Autowired(required = false)
    com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.kafka.AppUsersEventsKafkaListener listener;

    @Test
    void app_users_listener_is_loaded() {
        assertThat(listener).isNotNull();
    }


    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM user_social_projection");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
        outboxRepo.deleteAll();
    }

    @Test
    void google_login_publishes_appuser_events_to_kafka_and_social_projection_is_upserted() throws Exception {

        // GIVEN
        String authorizationCode = "userA";

        // WHEN : login (attention au DTO: authorizationCode)
        mockMvc.perform(
                        post("/auth/google/exchange")
                                .contentType("application/json")
                                .content("""
                                    { "authorizationCode": "%s" }
                                """.formatted(authorizationCode))
                )
                .andExpect(status().isOk());

        var outboxApp = jdbcTemplate.queryForList("""
          SELECT id, aggregate_type, event_type, status, stream_key
          FROM outbox_events
          WHERE aggregate_type='AppUser'
          ORDER BY id DESC
          LIMIT 10
        """);
        System.out.println("OUTBOX AppUser = " + outboxApp);
        assertThat(outboxApp).isNotEmpty();

        // WHEN : on pousse l'outbox vers Kafka
        outboxEventDispatcher.dispatchPending();
        var outboxLatest = jdbcTemplate.queryForList("""
          SELECT id, aggregate_type, event_type, status, retry_count
          FROM outbox_events
          ORDER BY id DESC
          LIMIT 20
        """);
        System.out.println("OUTBOX latest = " + outboxLatest);
        // THEN : attendre que socialContext read consomme app-users-events et upsert user_social_projection
        int retries = 25;

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_social_projection", Integer.class);
            assertThat(count).isEqualTo(1);
        });
        List<Map<String, Object>> rows;
        do {
            Thread.sleep(200);
            rows = jdbcTemplate.queryForList("SELECT * FROM user_social_projection");
        } while (rows.isEmpty() && --retries > 0);

        assertThat(rows).hasSize(1);

        var row = rows.get(0);
        assertThat(row.get("display_name")).isNotNull();
        // avatar_url peut être null si fake ne le met pas, mais chez toi il le met => on check non null
        assertThat(row.get("avatar_url")).isNotNull();
    }
}

package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = { "auth-users-events" },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
public class AuthUserKafkaIntegrationTest extends AbstractBaseE2E {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void authUserCreatedEvent_is_published_to_kafka_and_consumed_by_userContext() throws Exception {

        // GIVEN
        String code = "kafka-test-user";

        // WHEN : effectue un login Google
        mockMvc.perform(post("/auth/google/exchange")
                        .contentType("application/json")
                        .content("""
                                {
                                  "code": "%s",
                                  "codeVerifier": "dummy-verifier",
                                  "redirectUri": "com.fragments:/oauth2redirect"
                                }
                                """.formatted(code))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // THEN : attendre que le listener ait le temps de consommer et persister en DB
        // (ultra simple, on peut raffiner plus tard avec Awaitility)
        int retries = 10;
        List<Map<String, Object>> appUsers;
        do {
            Thread.sleep(200);
            appUsers = jdbcTemplate.queryForList("SELECT * FROM app_users");
        } while (appUsers.isEmpty() && --retries > 0);

        assertThat(appUsers).hasSize(1);
    }
}

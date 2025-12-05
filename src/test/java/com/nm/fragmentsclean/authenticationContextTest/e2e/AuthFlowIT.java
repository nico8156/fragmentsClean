package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
public class AuthFlowIT extends AbstractBaseE2E {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SpringOutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
        outboxEventRepository.deleteAll();
    }

    @Test
    void google_login_creates_auth_and_app_user_and_returns_tokens() throws Exception {
        // GIVEN
        var code = "test-code-123";

        // WHEN
        var mvcResult = mockMvc.perform(
                        post("/auth/google/exchange")
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
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.displayName").value("User " + code))
                .andExpect(jsonPath("$.user.email").value(code + "@example.com"))
                .andReturn();

        // THEN : on vérifie le JSON en détail
        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsByteArray());

        String accessToken = root.path("accessToken").asText();
        String refreshToken = root.path("refreshToken").asText();
        String userId = root.path("user").path("id").asText();

        assertThat(accessToken).startsWith("access-");
        assertThat(refreshToken).startsWith("refresh-");
        assertThat(userId).isNotBlank();

        // Sanity check DB : 1 auth_user + 1 app_user
        var authUsers = jdbcTemplate.queryForList("SELECT * FROM auth_users");
        var appUsers = jdbcTemplate.queryForList("SELECT * FROM app_users");

        assertThat(authUsers).hasSize(1);
        assertThat(appUsers).hasSize(1);

        // Optionnel : vérifier l’outbox a reçu des events
//        var outboxEvents = outboxEventRepository.findAll();
//        assertThat(outboxEvents).isNotEmpty();
    }
}

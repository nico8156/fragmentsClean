package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AuthRefreshIT extends AbstractBaseE2E {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
    }

    @Test
    void refresh_token_flow_works() throws Exception {
        // GIVEN : un premier login Google → access + refresh + user
        var code = "test-refresh-123";

        var loginResult = mockMvc.perform(
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
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());

        String accessToken1 = loginJson.path("accessToken").asText();
        String refreshToken1 = loginJson.path("refreshToken").asText();
        String userId = loginJson.path("user").path("id").asText();

        assertThat(accessToken1).isNotBlank();
        assertThat(refreshToken1).isNotBlank();
        assertThat(userId).isNotBlank();

        // WHEN : on appelle /auth/refresh avec le refreshToken1
        var refreshResult = mockMvc.perform(
                        post("/auth/refresh")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(refreshToken1))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsByteArray());

        String accessToken2 = refreshJson.path("accessToken").asText();
        String refreshToken2 = refreshJson.path("refreshToken").asText();

        // THEN : les nouveaux tokens sont bien renvoyés et le refresh a été roté
        assertThat(accessToken2).isNotBlank();
        assertThat(refreshToken2).isNotBlank();
        assertThat(accessToken2).isNotEqualTo(accessToken1);
        assertThat(refreshToken2).isNotEqualTo(refreshToken1);

        // Optionnel : vérif structure JWT
        assertThat(accessToken1.split("\\.")).hasSize(3);
        assertThat(accessToken2.split("\\.")).hasSize(3);

        // Sanity check DB : 1 auth_user, 1 app_user
        var authUsers = jdbcTemplate.queryForList("SELECT * FROM auth_users");
        var appUsers = jdbcTemplate.queryForList("SELECT * FROM app_users");
        assertThat(authUsers).hasSize(1);
        assertThat(appUsers).hasSize(1);

        // Sanity check DB : 2 refresh_tokens pour ce user (un révoqué, un actif)
        var refreshTokens = jdbcTemplate.queryForList(
                "SELECT token, revoked FROM refresh_tokens WHERE user_id = ?",
                java.util.UUID.fromString(userId)
        );
        assertThat(refreshTokens).hasSize(2);

        long revokedCount = refreshTokens.stream()
                .filter(row -> Boolean.TRUE.equals(row.get("revoked")))
                .count();
        long activeCount = refreshTokens.stream()
                .filter(row -> Boolean.FALSE.equals(row.get("revoked")))
                .count();

        assertThat(revokedCount).isEqualTo(1);
        assertThat(activeCount).isEqualTo(1);
    }

}

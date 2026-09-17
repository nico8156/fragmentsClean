package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway.JwtTokenService;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
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

    @SpyBean
    JwtTokenService tokenService;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
    }

    @AfterEach
    void resetTokenService() {
        reset(tokenService);
    }

    @Test
    void refresh_token_flow_works() throws Exception {
        // GIVEN : un premier login Google → access + refresh + user
        var authorizationCode = "test-refresh-123";

        var loginResult = mockMvc.perform(
                        post("/auth/google/mobile")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "authorizationCode": "%s",
                                      "codeVerifier": "verifier-%s",
                                      "redirectUri": "com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"
                                    }
                                    """.formatted(authorizationCode, authorizationCode))
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

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                            {"refreshToken":"%s"}
                            """.formatted(refreshToken1)))
                .andExpect(status().isUnauthorized());

        // Optionnel : vérif structure JWT
        assertThat(accessToken1.split("\\.")).hasSize(3);
        assertThat(accessToken2.split("\\.")).hasSize(3);

        // Sanity check DB : auth state is persisted. AppUser creation is handled by async consumers.
        var authUsers = jdbcTemplate.queryForList("SELECT * FROM auth_users");
        assertThat(authUsers).hasSize(1);

        // Sanity check DB : 2 refresh_tokens pour ce user (un révoqué, un actif)
        var refreshTokens = jdbcTemplate.queryForList(
                "SELECT token_hash, revoked FROM refresh_tokens WHERE user_id = ?",
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
        assertThat(refreshTokens)
                .allSatisfy(row -> assertThat(row.get("token_hash")).asString().hasSize(64));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name='refresh_tokens' AND column_name='token'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash IN (?, ?)",
                Integer.class,
                refreshToken1,
                refreshToken2)).isZero();
    }

    @Test
    void only_one_concurrent_rotation_of_the_same_refresh_token_succeeds() throws Exception {
        String refreshToken = loginAndGetRefreshToken("concurrent-refresh");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.of(1, 2).stream()
                    .map(ignored -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return refresh(refreshToken);
                    }))
                    .toList();

            ready.await();
            start.countDown();
            var statuses = requests.stream().map(future -> {
                try {
                    return future.get().getResponse().getStatus();
                } catch (Exception error) {
                    throw new AssertionError(error);
                }
            }).toList();

            assertThat(statuses).containsExactlyInAnyOrder(200, 401);
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE revoked=false", Integer.class)).isEqualTo(1);
    }

    @Test
    void failure_while_issuing_the_successor_rolls_back_the_consumption() throws Exception {
        String refreshToken = loginAndGetRefreshToken("refresh-rollback");
        doThrow(new IllegalStateException("simulated token issue failure"))
                .when(tokenService).generateTokensForUser(any(), any());

        assertThatThrownBy(() -> refresh(refreshToken))
                .hasRootCauseMessage("simulated token issue failure");

        reset(tokenService);
        assertThat(refresh(refreshToken).getResponse().getStatus()).isEqualTo(200);
    }

    private String loginAndGetRefreshToken(String authorizationCode) throws Exception {
        var result = mockMvc.perform(post("/auth/google/mobile")
                        .contentType("application/json")
                        .content("""
                            {
                              "authorizationCode": "%s",
                              "codeVerifier": "verifier-%s",
                              "redirectUri": "com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"
                            }
                            """.formatted(authorizationCode, authorizationCode)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("refreshToken").asText();
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                            {"refreshToken":"%s"}
                            """.formatted(refreshToken)))
                .andReturn();
    }

}

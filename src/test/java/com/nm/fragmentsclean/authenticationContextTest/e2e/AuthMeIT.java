package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
public class AuthMeIT extends AbstractBaseE2E {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void google_login_then_auth_me_returns_user_info() throws Exception {
        // GIVEN : login Google
        var code = "test-me-123";

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
                .andExpect(jsonPath("$.user.id").exists())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
        String accessToken = loginJson.path("accessToken").asText();
        String userId = loginJson.path("user").path("id").asText();

        // WHEN : appel /auth/me avec le Bearer
        var meResult = mockMvc.perform(
                        get("/auth/me")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andReturn();

        JsonNode meJson = objectMapper.readTree(meResult.getResponse().getContentAsByteArray());
        assertThat(meJson.path("userId").asText()).isEqualTo(userId);

        // Sanity : appel sans token => 401
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}

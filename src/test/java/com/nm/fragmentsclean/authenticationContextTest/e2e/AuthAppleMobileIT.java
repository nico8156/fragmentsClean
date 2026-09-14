package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class AuthAppleMobileIT extends AbstractBaseE2E {
  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
  @Autowired org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
  @MockBean AppleAuthService apple;

  @BeforeEach
  void clean() {
    when(apple.authenticate(anyString(), anyString(), anyString()))
        .thenAnswer(invocation -> new AppleAuthService.AppleUserInfo(
            "apple-reviewer", "apple@example.test", true, invocation.getArgument(2),
            "apple-refresh-reviewer"));
    jdbc.update("DELETE FROM auth_provider_credentials");
    jdbc.update("DELETE FROM refresh_tokens");
    jdbc.update("DELETE FROM app_users");
    jdbc.update("DELETE FROM auth_users");
    jdbc.update("DELETE FROM outbox_events");
  }

  @Test
  void apple_without_email_can_create_a_session_and_reconnect_to_the_same_identity() throws Exception {
    when(apple.authenticate(anyString(), anyString(), anyString()))
        .thenReturn(new AppleAuthService.AppleUserInfo(
            "apple-no-email", null, false, "Camille", "provider-refresh"));
    String request = """
        {"identityToken":"verified-at-provider-boundary","authorizationCode":"fresh-code","displayName":"Camille"}
        """;
    for (int i = 0; i < 2; i++) {
      var response = mockMvc.perform(post("/auth/apple/mobile").contentType("application/json").content(request))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").isNotEmpty())
          .andExpect(jsonPath("$.user.displayName").value("Camille"))
          .andReturn().getResponse();
      var body = json.readTree(response.getContentAsString());
      assertThat(jwtDecoder.decode(body.get("accessToken").asText()).getClaims()).doesNotContainKey("email");
      mockMvc.perform(post("/auth/refresh").contentType("application/json")
          .content(json.writeValueAsString(java.util.Map.of("refreshToken", body.get("refreshToken").asText()))))
          .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_users", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT email FROM auth_users", String.class)).isNull();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_provider_credentials", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isPositive();
  }

  @Test
  void exchanges_a_native_apple_credential_and_never_stores_the_provider_token_in_plaintext()
      throws Exception {
    mockMvc
        .perform(
            post("/auth/apple/mobile")
                .contentType("application/json")
                .content(
                    """
{"identityToken":"identity-token","authorizationCode":"reviewer","displayName":"Nicolas"}
"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.displayName").value("Nicolas"));

    assertThat(jdbc.queryForObject("SELECT provider FROM auth_users", String.class))
        .isEqualTo("APPLE");
    assertThat(
            jdbc.queryForObject(
                "SELECT encrypted_refresh_token FROM auth_provider_credentials", String.class))
        .doesNotContain("apple-refresh-reviewer");
  }
}

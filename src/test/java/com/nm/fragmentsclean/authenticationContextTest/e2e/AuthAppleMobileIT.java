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

class AuthAppleMobileIT extends AbstractBaseE2E {
  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM auth_provider_credentials");
    jdbc.update("DELETE FROM refresh_tokens");
    jdbc.update("DELETE FROM app_users");
    jdbc.update("DELETE FROM auth_users");
    jdbc.update("DELETE FROM outbox_events");
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

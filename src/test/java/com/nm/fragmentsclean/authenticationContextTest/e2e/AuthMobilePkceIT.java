package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthMobilePkceIT extends AbstractBaseE2E {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	JwtDecoder jwtDecoder;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM refresh_tokens");
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
	}

	@Test
	void mobile_pkce_login_creates_auth_user_and_returns_fragments_session() throws Exception {
		var code = "mobile-pkce-code-123";

		var result = mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "%s",
								  "codeVerifier": "verifier-123",
								  "redirectUri": "fragmentscleanfront://auth/google"
								}
								""".formatted(code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.user.id").isNotEmpty())
				.andExpect(jsonPath("$.user.displayName").value("User " + code))
				.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
		String accessToken = body.path("accessToken").asText();
		String refreshToken = body.path("refreshToken").asText();
		String userId = body.path("user").path("id").asText();

		Jwt jwt = jwtDecoder.decode(accessToken);
		assertThat(jwt.getSubject()).isEqualTo(userId);
		assertThat(refreshToken).isNotBlank();
		assertThat(jdbcTemplate.queryForList("SELECT * FROM auth_users")).hasSize(1);
		assertThat(jdbcTemplate.queryForList("SELECT * FROM refresh_tokens")).hasSize(1);
	}

	@Test
	void mobile_pkce_login_rejects_blank_required_fields() throws Exception {
		mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "",
								  "codeVerifier": "verifier-123",
								  "redirectUri": "fragmentscleanfront://auth/google"
								}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "code-123",
								  "codeVerifier": " ",
								  "redirectUri": "fragmentscleanfront://auth/google"
								}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "code-123",
								  "codeVerifier": "verifier-123",
								  "redirectUri": ""
								}
								"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "code-123",
								  "codeVerifier": "verifier-123",
								  "redirectUri": "fragmentsclean://auth/google"
								}
								"""))
				.andExpect(status().isBadRequest());
	}
}

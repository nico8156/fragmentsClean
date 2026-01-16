package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;

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

	@Autowired
	JwtDecoder jwtDecoder;

	@BeforeEach
	void setup() {
		// ordre important si FK
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
		outboxEventRepository.deleteAll();
	}

	@Test
	void google_login_creates_auth_and_app_user_and_returns_tokens() throws Exception {
		// GIVEN
		String authorizationCode = "test-code-123";

		// WHEN
		var mvcResult = mockMvc.perform(
				post("/auth/google/exchange")
						.contentType("application/json")
						.content("""
								{ "authorizationCode": "%s" }
								""".formatted(authorizationCode)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.user.id").isNotEmpty())
				.andExpect(jsonPath("$.user.displayName").value("User " + authorizationCode))
				.andExpect(jsonPath("$.user.email").value(authorizationCode + "@example.com"))
				.andReturn();

		JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsByteArray());

		String accessToken = root.path("accessToken").asText();
		String refreshToken = root.path("refreshToken").asText();
		String userId = root.path("user").path("id").asText();

		assertThat(accessToken).isNotBlank();
		assertThat(refreshToken).isNotBlank();
		assertThat(userId).isNotBlank();

		// JWT sanity check
		assertThat(accessToken.split("\\.")).hasSize(3);

		// Decode JWT
		Jwt jwt = jwtDecoder.decode(accessToken);

		// ✅ identity invariant (front uses sub as userId)
		assertThat(jwt.getSubject()).isEqualTo(userId);

		// issuer
		assertThat(jwt.getIssuer().toString()).isEqualTo("https://auth.fragments");

		// roles/scopes
		List<String> roles = jwt.getClaimAsStringList("roles");
		List<String> scopes = jwt.getClaimAsStringList("scopes");

		assertThat(roles).isNotNull().containsExactly("USER");
		assertThat(scopes).isNotNull().contains("gamification:read", "gamification:earn");

		// DB sanity
		String authUserIdDb = jdbcTemplate.queryForObject(
				"SELECT id FROM auth_users LIMIT 1",
				String.class);
		String appUserIdDb = jdbcTemplate.queryForObject(
				"SELECT id FROM app_users LIMIT 1",
				String.class);

		assertThat(authUserIdDb).isNotBlank();
		assertThat(appUserIdDb).isNotBlank();

		// ✅ auth_user.id == jwt.sub
		assertThat(authUserIdDb).isEqualTo(jwt.getSubject());

		// counts
		var authUsers = jdbcTemplate.queryForList("SELECT * FROM auth_users");
		var appUsers = jdbcTemplate.queryForList("SELECT * FROM app_users");

		assertThat(authUsers).hasSize(1);
		assertThat(appUsers).hasSize(1);
	}
}

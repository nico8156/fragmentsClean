package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.sqs.AuthUserCreatedSqsIntegrationEventHandler;

@ActiveProfiles("test")
public class AuthMeIT extends AbstractBaseE2E {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	SpringOutboxEventRepository outboxEventRepository;
	@Autowired
	TransactionTemplate transactionTemplate;
	@Autowired
	AuthUserCreatedSqsIntegrationEventHandler authUserCreatedSqsIntegrationEventHandler;

	@BeforeEach
	void setup() {
		// optionnel mais ça évite les interférences
		jdbcTemplate.update("DELETE FROM refresh_tokens");
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
		jdbcTemplate.update("DELETE FROM outbox_events");
	}

	@Test
	void google_login_then_auth_me_returns_user_info_after_async_pipeline() throws Exception {
		// GIVEN : login Google
		var code = "test-me-123";

		var loginResult = mockMvc.perform(
				post("/auth/google/mobile")
						.contentType("application/json")
						.content("""
								{
								  "authorizationCode": "%s",
								  "codeVerifier": "verifier-%s",
								  "redirectUri": "com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"
								}
								""".formatted(code, code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.user.id").exists())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
		String accessToken = loginJson.path("accessToken").asText();
		String appUserId = loginJson.path("user").path("id").asText();

		// WHEN : outbox -> public integration envelope -> SQS consumer ACL
		var envelope = transactionTemplate.execute(status -> {
			var outboxEvent = outboxEventRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING).getFirst();
			return new IntegrationEventEnvelopeFactory(objectMapper)
					.from(outboxEvent, IntegrationEventDestinations.AUTH_USERS_EVENTS);
		});
		authUserCreatedSqsIntegrationEventHandler.handle(envelope);

		// THEN : attendre que app_users soit créé (pipeline async)
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			Integer count = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM app_users WHERE id = ?",
					Integer.class,
					java.util.UUID.fromString(appUserId));
			assertThat(count).isEqualTo(1);
			String displayName = jdbcTemplate.queryForObject(
					"SELECT display_name FROM app_users WHERE id = ?",
					String.class,
					java.util.UUID.fromString(appUserId));
			assertThat(displayName).isEqualTo("User " + code);
		});

		// WHEN/THEN : appel /auth/me avec le Bearer => 200
		mockMvc.perform(
				get("/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(appUserId));

		// Sanity : appel sans token => 401
		mockMvc.perform(get("/auth/me"))
				.andExpect(status().isUnauthorized());
	}
}

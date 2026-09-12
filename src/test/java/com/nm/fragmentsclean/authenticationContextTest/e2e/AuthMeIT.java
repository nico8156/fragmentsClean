package com.nm.fragmentsclean.authenticationContextTest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouter;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserDeletionRequestedEvent;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
public class AuthMeIT extends AbstractBaseE2E {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired SpringOutboxEventRepository outboxEventRepository;
  @Autowired TransactionTemplate transactionTemplate;
  @Autowired SqsIntegrationEventRouter eventRouter;

  @BeforeEach
  void setup() {
    // optionnel mais ça évite les interférences
    jdbcTemplate.update("DELETE FROM refresh_tokens");
    jdbcTemplate.update("DELETE FROM auth_provider_credentials");
    jdbcTemplate.update("DELETE FROM account_deletion_processes");
    jdbcTemplate.update("DELETE FROM inbox_messages");
    jdbcTemplate.update("DELETE FROM app_users");
    jdbcTemplate.update("DELETE FROM auth_users");
    jdbcTemplate.update("DELETE FROM outbox_events");
  }

  @Test
  void google_login_then_auth_me_returns_user_info_after_async_pipeline() throws Exception {
    // GIVEN : login Google
    var code = "test-me-123";

    var loginResult =
        mockMvc
            .perform(
                post("/auth/google/mobile")
                    .contentType("application/json")
                    .content(
                        """
{
  "authorizationCode": "%s",
  "codeVerifier": "verifier-%s",
  "redirectUri": "com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"
}
"""
                            .formatted(code, code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.user.id").exists())
            .andReturn();

    JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
    String accessToken = loginJson.path("accessToken").asText();
    String appUserId = loginJson.path("user").path("id").asText();

    // WHEN : outbox -> public integration envelope -> SQS consumer ACL
    var envelope =
        transactionTemplate.execute(
            status -> {
              var outboxEvent =
                  outboxEventRepository
                      .findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING)
                      .getFirst();
              return new IntegrationEventEnvelopeFactory(objectMapper)
                  .from(outboxEvent, IntegrationEventDestinations.AUTH_USERS_EVENTS);
            });
    eventRouter.route(envelope);
    eventRouter.route(envelope);

    // THEN : attendre que app_users soit créé (pipeline async)
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              Integer count =
                  jdbcTemplate.queryForObject(
                      "SELECT COUNT(*) FROM app_users WHERE id = ?",
                      Integer.class,
                      java.util.UUID.fromString(appUserId));
              assertThat(count).isEqualTo(1);
              String displayName =
                  jdbcTemplate.queryForObject(
                      "SELECT display_name FROM app_users WHERE id = ?",
                      String.class,
                      java.util.UUID.fromString(appUserId));
              assertThat(displayName).isEqualTo("User " + code);
            });

    // WHEN/THEN : le profil produit est lu depuis son bounded context propriétaire
    mockMvc
        .perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(appUserId));

    // Sanity : appel sans token => 401
    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

    // The request is durable first; the authentication consumer then invalidates
    // both refresh tokens and access tokens which have already been issued.
    var deletionCommandId = java.util.UUID.randomUUID();
    mockMvc
        .perform(
            delete("/api/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .content(
                    """
                    {"commandId":"%s"}
                    """
                        .formatted(deletionCommandId)))
        .andExpect(status().isAccepted());

    var deletionEnvelope =
        transactionTemplate.execute(
            status -> {
              var event =
                  outboxEventRepository.findAll().stream()
                      .filter(
                          item ->
                              item.getEventType()
                                  .equals(AppUserDeletionRequestedEvent.class.getName()))
                      .findFirst()
                      .orElseThrow();
              return new IntegrationEventEnvelopeFactory(objectMapper)
                  .from(event, IntegrationEventDestinations.AUTH_USERS_EVENTS);
            });
    eventRouter.route(deletionEnvelope);
    eventRouter.route(deletionEnvelope);

    mockMvc
        .perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
  }
}

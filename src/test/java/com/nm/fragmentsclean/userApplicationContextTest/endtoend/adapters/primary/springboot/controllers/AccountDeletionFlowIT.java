package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouter;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserDeletionRequestedEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

class AccountDeletionFlowIT extends AbstractBaseE2E {
  private static final UUID USER_ID = UUID.fromString("31111111-1111-4111-8111-111111111111");
  private static final UUID REQUEST_ID = UUID.fromString("32222222-2222-4222-8222-222222222222");
  private static final UUID CONTENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper objectMapper;
  @Autowired SpringOutboxEventRepository outbox;
  @Autowired SqsIntegrationEventRouter router;
  @Autowired TransactionTemplate transactions;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM inbox_messages");
    jdbc.update("DELETE FROM command_status");
    jdbc.update("DELETE FROM account_deletion_processes");
    jdbc.update("DELETE FROM auth_provider_credentials");
    jdbc.update("DELETE FROM refresh_tokens");
    jdbc.update("DELETE FROM saved_coffees");
    jdbc.update("DELETE FROM comments");
    jdbc.update("DELETE FROM users");
    jdbc.update("DELETE FROM tickets");
    jdbc.update("DELETE FROM experience_reports_projection");
    jdbc.update("DELETE FROM experience_reports");
    jdbc.update("DELETE FROM experience_views");
    jdbc.update("DELETE FROM experiences");
    jdbc.update("DELETE FROM experience_user_profiles");
    outbox.deleteAll();
    jdbc.update("DELETE FROM app_users");
    jdbc.update("DELETE FROM auth_users");
    seedOwnedData();
  }

  @Test
  void erases_every_current_bounded_context_and_completes_after_duplicate_safe_acknowledgements()
      throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me")
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token.subject(USER_ID.toString()).claim("roles", List.of("USER"))))
                .contentType("application/json")
                .content(
                    """
                    {"commandId":"%s"}
                    """
                        .formatted(REQUEST_ID)))
        .andExpect(status().isAccepted());

    OutboxEventJpaEntity requested = event(AppUserDeletionRequestedEvent.class.getName());
    var envelopes =
        List.of(
            envelope(requested, IntegrationEventDestinations.APP_USERS_EVENTS),
            envelope(requested, IntegrationEventDestinations.AUTH_USERS_EVENTS),
            envelope(requested, IntegrationEventDestinations.DOMAIN_EVENTS),
            envelope(requested, IntegrationEventDestinations.TICKET_EVENTS),
            envelope(requested, IntegrationEventDestinations.EXPERIENCES_EVENTS));
    envelopes.forEach(router::route);
    envelopes.forEach(router::route);

    List<OutboxEventJpaEntity> acknowledgements =
        outbox.findAll().stream()
            .filter(event -> event.getEventType().endsWith("AccountDataErasedEvent"))
            .toList();
    assertThat(acknowledgements).hasSize(4);
    acknowledgements.stream()
        .map(event -> envelope(event, IntegrationEventDestinations.APP_USERS_EVENTS))
        .forEach(router::route);

    assertThat(count("saved_coffees", "user_id")).isZero();
    assertThat(count("comments", "author_id")).isZero();
    assertThat(count("users", "user_id")).isZero();
    assertThat(count("tickets", "user_id")).isZero();
    assertThat(count("experiences", "user_id")).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT revoked FROM refresh_tokens WHERE user_id = ?", Boolean.class, USER_ID))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT lifecycle_status FROM auth_users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("DELETED");
    assertThat(
            jdbc.queryForObject("SELECT email FROM auth_users WHERE id = ?", String.class, USER_ID))
        .startsWith("deleted+");
    assertThat(
            jdbc.queryForObject(
                "SELECT lifecycle_status FROM app_users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("DELETED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM account_deletion_processes WHERE request_id = ?",
                String.class,
                REQUEST_ID))
        .isEqualTo("COMPLETED");
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inbox_messages WHERE event_id = ? AND status = 'PROCESSED'",
                Integer.class,
                requested.getEventId()))
        .isEqualTo(5);
  }

  private OutboxEventJpaEntity event(String eventType) {
    return outbox.findAll().stream()
        .filter(event -> event.getEventType().equals(eventType))
        .findFirst()
        .orElseThrow();
  }

  private com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope
      envelope(OutboxEventJpaEntity event, String destination) {
    return transactions.execute(
        status -> new IntegrationEventEnvelopeFactory(objectMapper).from(event, destination));
  }

  private int count(String table, String ownerColumn) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + ownerColumn + " = ?", Integer.class, USER_ID);
  }

  private void seedOwnedData() {
    var at = Timestamp.from(NOW);
    jdbc.update(
        """
INSERT INTO auth_users (
  id, provider, provider_user_id, email, email_verified, display_name, avatar_url, last_login_at
) VALUES (?, 'GOOGLE', ?, 'deletion@example.test', true, 'Deletion Test', null, ?)
""",
        USER_ID,
        USER_ID.toString(),
        at);
    jdbc.update(
        """
INSERT INTO app_users (id, auth_user_id, display_name, avatar_url, created_at, updated_at, version)
VALUES (?, ?, 'Deletion Test', null, ?, ?, 0)
""",
        USER_ID,
        USER_ID,
        at,
        at);
    jdbc.update(
        """
        INSERT INTO refresh_tokens (id, user_id, token, expires_at, revoked)
        VALUES (?, ?, 'refresh-deletion-test', ?, false)
        """,
        CONTENT_ID,
        USER_ID,
        Timestamp.from(NOW.plusSeconds(3600)));
    jdbc.update(
        """
INSERT INTO saved_coffees (saved_coffee_id, user_id, coffee_id, active, updated_at, version)
VALUES (?, ?, ?, true, ?, 0)
""",
        CONTENT_ID,
        USER_ID,
        UUID.randomUUID(),
        at);
    jdbc.update(
        """
INSERT INTO comments (comment_id, target_id, author_id, body, created_at, moderation, version)
VALUES (?, ?, ?, 'Personal comment', ?, 'PUBLISHED', 0)
""",
        CONTENT_ID,
        UUID.randomUUID(),
        USER_ID,
        at);
    jdbc.update(
        """
INSERT INTO users (user_id, created_at, updated_at, display_name, avatar_url, bio, locale, version)
VALUES (?, ?, ?, 'Deletion Test', null, 'Personal bio', 'fr-FR', 0)
""",
        USER_ID,
        at,
        at);
    jdbc.update(
        """
        INSERT INTO tickets (
          ticket_id, user_id, status, currency, created_at, updated_at, version
        ) VALUES (?, ?, 'PENDING', 'EUR', ?, ?, 0)
        """,
        CONTENT_ID,
        USER_ID,
        at,
        at);
    jdbc.update(
        """
        INSERT INTO experiences (
          experience_id, user_id, coffee_id, message, publication_status,
          moderation_status, created_at, updated_at, version
        ) VALUES (?, ?, ?, 'Personal experience', 'PUBLISHED', 'VISIBLE', ?, ?, 0)
        """,
        UUID.randomUUID(),
        USER_ID,
        UUID.randomUUID(),
        at,
        at);
  }
}

package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class UserProfileControllerIT extends AbstractBaseE2E {
  private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID COMMAND_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired SpringOutboxEventRepository outbox;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM command_status");
    jdbc.update("DELETE FROM account_deletion_processes");
    outbox.deleteAll();
    jdbc.update("DELETE FROM user_avatar_media");
    jdbc.update("DELETE FROM app_users");
    jdbc.update("DELETE FROM auth_users");
    seedUser();
  }

  @Test
  void reads_the_authenticated_product_profile_from_its_own_context() throws Exception {
    mockMvc
        .perform(get("/api/users/me").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
        .andExpect(jsonPath("$.displayName").value("Nicolas"))
        .andExpect(jsonPath("$.avatarUrl").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));

    mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void updates_the_profile_through_a_durable_authenticated_command() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/me/profile")
                .with(userJwt())
                .contentType("application/json")
                .content(
                    """
                    {"commandId":"%s","displayName":"  Nicolas   Maldiney  "}
                    """
                        .formatted(COMMAND_ID)))
        .andExpect(status().isAccepted());

    assertThat(
            jdbc.queryForObject(
                "SELECT display_name FROM app_users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("Nicolas Maldiney");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM command_status WHERE command_id = ?", String.class, COMMAND_ID))
        .isEqualTo("APPLIED");
    assertThat(outbox.findAll())
        .singleElement()
        .satisfies(
            event ->
                assertThat(event.getEventType())
                    .isEqualTo(AppUserProfileUpdatedEvent.class.getName()));
  }

  @Test
  void persists_an_explicit_profile_rejection_for_mobile_reconciliation() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/me/profile")
                .with(userJwt())
                .contentType("application/json")
                .content(
                    """
                    {"commandId":"%s","displayName":"x"}
                    """
                        .formatted(COMMAND_ID)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.reason").value("INVALID_DISPLAY_NAME"));

    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM command_status WHERE command_id = ?", String.class, COMMAND_ID))
        .isEqualTo("REJECTED");
  }

  @Test
  void rejects_a_missing_command_identifier_at_the_http_boundary() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/me/profile")
                .with(userJwt())
                .contentType("application/json")
                .content(
                    """
                    {"displayName":"Nicolas Maldiney"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void uploads_replaces_reads_and_removes_a_private_avatar() throws Exception {
    UUID mediaId=UUID.randomUUID();UUID confirm=UUID.randomUUID();
    mockMvc.perform(post("/api/users/me/avatar/upload-intents").with(userJwt()).contentType("application/json").content("""
        {"mediaId":"%s","contentType":"image/png","size":2048}
        """.formatted(mediaId))).andExpect(status().isCreated()).andExpect(jsonPath("$.uploadRequired").value(true));
    mockMvc.perform(post("/api/users/me/avatar/{mediaId}/confirm",mediaId).with(userJwt()).contentType("application/json").content("""
        {"commandId":"%s","at":"2026-09-11T10:01:00Z"}
        """.formatted(confirm))).andExpect(status().isAccepted());
    mockMvc.perform(get("/api/users/me").with(userJwt())).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("https://download.test/")));
	UUID replacementId=UUID.randomUUID();UUID replacementCommand=UUID.randomUUID();
	mockMvc.perform(post("/api/users/me/avatar/upload-intents").with(userJwt()).contentType("application/json").content("""
		{"mediaId":"%s","contentType":"image/jpeg","size":1024}
		""".formatted(replacementId))).andExpect(status().isCreated());
	mockMvc.perform(post("/api/users/me/avatar/{mediaId}/confirm",replacementId).with(userJwt()).contentType("application/json").content("""
		{"commandId":"%s","at":"2026-09-11T10:01:30Z"}
		""".formatted(replacementCommand))).andExpect(status().isAccepted());
	assertThat(jdbc.queryForObject("SELECT count(*) FROM user_avatar_media WHERE user_id=? AND status='AVAILABLE'",Integer.class,USER_ID)).isEqualTo(1);
	assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,mediaId)).isEqualTo("DELETION_PENDING");
    UUID remove=UUID.randomUUID();mockMvc.perform(delete("/api/users/me/avatar").with(userJwt()).contentType("application/json").content("""
        {"commandId":"%s","at":"2026-09-11T10:02:00Z"}
        """.formatted(remove))).andExpect(status().isAccepted());
    mockMvc.perform(get("/api/users/me").with(userJwt())).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").doesNotExist());
  }

  @Test
  void starts_a_durable_account_deletion_process_and_immediately_hides_the_profile()
      throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me")
                .with(userJwt())
                .contentType("application/json")
                .content(
                    """
                    {"commandId":"%s"}
                    """
                        .formatted(COMMAND_ID)))
        .andExpect(status().isAccepted());

    assertThat(
            jdbc.queryForObject(
                "SELECT lifecycle_status FROM app_users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("DELETION_REQUESTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM account_deletion_processes WHERE request_id = ?",
                String.class,
                COMMAND_ID))
        .isEqualTo("IN_PROGRESS");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM command_status WHERE command_id = ?", String.class, COMMAND_ID))
        .isEqualTo("APPLIED");
    mockMvc.perform(get("/api/users/me").with(userJwt())).andExpect(status().isNotFound());
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      userJwt() {
    return jwt()
        .jwt(token -> token.subject(USER_ID.toString()).claim("roles", java.util.List.of("USER")));
  }

  private void seedUser() {
    var at = Timestamp.from(Instant.parse("2026-09-11T10:00:00Z"));
    jdbc.update(
        """
INSERT INTO auth_users (
  id, provider, provider_user_id, email, email_verified, display_name, avatar_url, last_login_at
) VALUES (?, 'GOOGLE', ?, 'nico@example.test', true, 'Nicolas', null, ?)
""",
        USER_ID,
        USER_ID.toString(),
        at);
    jdbc.update(
        """
        INSERT INTO app_users (
          id, auth_user_id, display_name, avatar_url, created_at, updated_at, version
        ) VALUES (?, ?, 'Nicolas', null, ?, ?, 0)
        """,
        USER_ID,
        USER_ID,
        at,
        at);
  }
}

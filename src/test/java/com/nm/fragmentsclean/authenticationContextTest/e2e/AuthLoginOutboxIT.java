package com.nm.fragmentsclean.authenticationContextTest.e2e;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class AuthLoginOutboxIT extends AbstractBaseE2E {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SpringOutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventDispatcher outboxEventDispatcher;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM app_users");
        jdbcTemplate.update("DELETE FROM auth_users");
        outboxEventRepository.deleteAll();
    }

    @Test
    void google_login_persists_auth_events_in_outbox() throws Exception {
        // GIVEN
        var code = "outbox-login-123";

        // WHEN : login Google
        mockMvc.perform(
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
                .andExpect(status().isOk());

        // WHEN : on déclenche manuellement le dispatcher outbox → eventBus → projection
        outboxEventDispatcher.dispatchPending();
        // THEN : des events ont été écrits dans l’outbox
        var allEvents = outboxEventRepository.findAll();
        assertThat(allEvents).isNotEmpty();

        // On filtre sur aggregateType = "AuthUser"
        var authEvents = allEvents.stream()
                .filter(e -> "AuthUser".equals(e.getAggregateType()))
                .toList();

        assertThat(authEvents)
                .as("On doit avoir au moins un événement AuthUser en outbox")
                .isNotEmpty();
    }
    @Test
    void google_login_persists_auth_events_in_outbox_with_2_login() throws Exception {
        var code = "outbox-login-123";

        // 1) Premier login
        mockMvc.perform(
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
                .andExpect(status().isOk());

        var eventsAfterFirstLogin = outboxEventRepository.findAll();
        var typesAfterFirstLogin = eventsAfterFirstLogin.stream()
                .map(e -> e.getEventType())
                .toList();

        assertThat(typesAfterFirstLogin).contains(
                AuthUserCreatedEvent.class.getName(),
                AppUserCreatedEvent.class.getName()
        );

        // 2) Deuxième login avec le même code → même google.sub → même AuthUser
        mockMvc.perform(
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
                .andExpect(status().isOk());

        var eventsAfterSecondLogin = outboxEventRepository.findAll();
        var typesAfterSecondLogin = eventsAfterSecondLogin.stream()
                .map(e -> e.getEventType())
                .toList();

        assertThat(typesAfterSecondLogin)
                .anyMatch(t -> t.equals(AuthUserLoggedInEvent.class.getName()));
    }

}

package com.nm.fragmentsclean.authContext.e2e;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.events.UserAuthenticatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "spring.task.scheduling.enabled=false"
})
class AuthOutboxEventIT extends AbstractAuthBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringOutboxEventRepository outboxRepository;

    private static final String PROVIDER = "google";
    private static final String VALID_ID_TOKEN = "VALID_FAKE_TOKEN";

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void login_should_create_pending_outbox_event() throws Exception {
        // 1️⃣ On déclenche VRAIMENT le use case via le contrôleur
        String body = """
            {
              "provider": "%s",
              "idToken": "%s",
              "scopes": ["profile", "email"]
            }
            """.formatted(PROVIDER, VALID_ID_TOKEN);

        mockMvc.perform(
                        post("/api/auth/session/login")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.provider").value(PROVIDER));

        // 2️⃣ Maintenant seulement, on regarde la table outbox_events
        List<OutboxEventJpaEntity> events = outboxRepository.findAll();
        assertThat(events)
                .as("Un login doit produire au moins un event outbox")
                .isNotEmpty();

        OutboxEventJpaEntity event = events.get(0);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType()).isEqualTo(UserAuthenticatedEvent.class.getName());
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getAggregateId()).isNotBlank();
        assertThat(event.getStreamKey()).startsWith("user:");

        assertThat(event.getPayloadJson()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }
    @Test
    void refresh_should_create_pending_outbox_event() throws Exception {
        // 1️⃣ On part d’une base outbox vide
        outboxRepository.deleteAll();

        String body = """
        {
          "provider": "%s",
          "idToken": "%s",
          "scopes": ["profile", "email"]
        }
        """.formatted(PROVIDER, VALID_ID_TOKEN);

        // 2️⃣ On appelle le endpoint /session/refresh
        mockMvc.perform(
                        post("/api/auth/session/refresh")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.provider").value(PROVIDER));

        // 3️⃣ On vérifie que l’outbox contient au moins un event
        List<OutboxEventJpaEntity> events = outboxRepository.findAll();
        assertThat(events)
                .as("Un refresh doit produire au moins un event outbox")
                .isNotEmpty();

        OutboxEventJpaEntity event = events.get(0);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getEventType())
                .isEqualTo(UserAuthenticatedEvent.class.getName());
        assertThat(event.getAggregateType()).isEqualTo("User");
        assertThat(event.getAggregateId()).isNotBlank();
        assertThat(event.getStreamKey()).startsWith("user:");

        assertThat(event.getPayloadJson()).isNotBlank();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }

}

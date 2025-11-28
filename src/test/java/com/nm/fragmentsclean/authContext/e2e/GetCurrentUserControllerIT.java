package com.nm.fragmentsclean.authContext.e2e;

import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.entities.IdentityJpaEntity;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.SpringUserRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities.UserJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GetCurrentUserControllerIT extends AbstractAuthBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringUserRepository springUserRepository;

    @Autowired
    private SpringIdentityRepository springIdentityRepository;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    private UUID currentUserId;

    @BeforeEach
    void setup() {
        springIdentityRepository.deleteAll();
        springUserRepository.deleteAll();

        // On ne devine pas l’UUID : on demande au CurrentUserProvider
        currentUserId = currentUserProvider.currentUserId();

        Instant now = Instant.parse("2024-01-01T10:00:00Z");

        UserJpaEntity user = new UserJpaEntity(
                currentUserId,
                now,
                now,
                "Current User",
                "https://example.com/avatar.png",
                null,          // bio
                "fr-FR",
                1L
        );
        springUserRepository.save(user);

        IdentityJpaEntity identity = new IdentityJpaEntity(
                UUID.randomUUID(),
                currentUserId,
                "google",
                "fake-user-1101669198",
                "fake-user-1101669198@example.test",
                now,
                now
        );
        springIdentityRepository.save(identity);
    }

    @Test
    void should_return_current_user_snapshot() throws Exception {
        mockMvc.perform(
                        get("/api/auth/session/me")
                                .accept(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))

                .andExpect(jsonPath("$.user.id", is(currentUserId.toString())))
                .andExpect(jsonPath("$.user.displayName", is("Current User")))
                .andExpect(jsonPath("$.user.identities", hasSize(1)))
                .andExpect(jsonPath("$.user.identities[0].provider", is("google")))
                .andExpect(jsonPath("$.serverTime").isNotEmpty());
    }
}

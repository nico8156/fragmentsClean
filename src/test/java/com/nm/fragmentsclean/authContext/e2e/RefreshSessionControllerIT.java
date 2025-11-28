package com.nm.fragmentsclean.authContext.e2e;

import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.entities.IdentityJpaEntity;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.SpringUserRepository;
import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RefreshSessionControllerIT extends AbstractAuthBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringIdentityRepository springIdentityRepository;

    @Autowired
    private SpringUserRepository springUserRepository;

    // ⚠️ Ces constantes doivent matcher ce que renvoie ton FakeOAuthIdTokenVerifier
    // Dans RefreshSessionControllerIT

    private static final String PROVIDER = "google";
    private static final String VALID_ID_TOKEN = "VALID_FAKE_TOKEN";

    private static final String PROVIDER_USER_ID = "fake-user-1101669198";
    private static final String EMAIL = "fake-user-1101669198@example.test";
    private static final String DISPLAY_NAME = "Fake Google";

    private static final UUID EXISTING_USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");


    @BeforeEach
    void setup() {
        springIdentityRepository.deleteAll();
        springUserRepository.deleteAll();
    }
    @Test
    void should_create_user_and_identity_on_first_login_without_refresh() throws Exception {

        String body = """
    {
        "provider": "google",
        "idToken": "VALID_FAKE_TOKEN",
        "scopes": ["profile","email"]
    }
    """;

        mockMvc.perform(
                        post("/api/auth/session/login")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.identities", hasSize(1)))
                .andExpect(jsonPath("$.provider").value("google"));
    }


    @Test
    void should_create_user_and_identity_on_first_login() throws Exception {
        // Aucun user ni identity au départ

        String body = """
            {
              "provider": "%s",
              "idToken": "%s",
              "scopes": ["profile", "email"]
            }
            """.formatted(PROVIDER, VALID_ID_TOKEN);

        mockMvc.perform(
                        post("/api/auth/session/refresh")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))

                // Access token présent
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokens.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.tokens.scope").value("openid profile email"))


                // User présent dans la réponse
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.displayName").value(DISPLAY_NAME))

                // Provider renvoyé
                .andExpect(jsonPath("$.provider").value(PROVIDER));
        // adapte le reste suivant ton RefreshSessionResponseDto
    }

    @Test
    void should_reuse_existing_user_and_identity_on_next_login() throws Exception {
        Instant createdAt = Instant.parse("2024-01-01T10:00:00Z");
        Instant updatedAt = createdAt;
        long version = 1L;

        // 1. User existant cohérent avec le snapshot attendu
        UserJpaEntity user = new UserJpaEntity(
                EXISTING_USER_ID,
                createdAt,
                updatedAt,
                DISPLAY_NAME,
                "https://example.com/avatar/fake-user-1101669198.png",
                null,          // bio
                "fr-FR",
                version
        );
        user = springUserRepository.save(user);

        // 2. Identity existante liée à ce user, cohérente avec le fake
        IdentityJpaEntity identity = new IdentityJpaEntity(
                UUID.randomUUID(),
                user.getId(),
                PROVIDER,
                PROVIDER_USER_ID,
                EMAIL,
                createdAt,
                null // lastAuthAt
        );
        springIdentityRepository.save(identity);

        String body = """
        {
          "provider": "%s",
          "idToken": "%s",
          "scopes": ["profile", "email"]
        }
        """.formatted(PROVIDER, VALID_ID_TOKEN);

        mockMvc.perform(
                        post("/api/auth/session/refresh")
                                .contentType(APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))

                // token dans $.tokens.accessToken
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())

                // ⬇️ maintenant on peut raisonnablement attendre la réutilisation du même user
                .andExpect(jsonPath("$.user.id").value(EXISTING_USER_ID.toString()))
                .andExpect(jsonPath("$.user.displayName").value(DISPLAY_NAME))

                .andExpect(jsonPath("$.provider").value(PROVIDER));
    }


}

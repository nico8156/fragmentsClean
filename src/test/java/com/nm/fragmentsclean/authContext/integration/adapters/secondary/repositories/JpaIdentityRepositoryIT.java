package com.nm.fragmentsclean.authContext.integration.adapters.secondary.repositories;


import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.SpringIdentityRepository;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.entities.IdentityJpaEntity;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;

import com.nm.fragmentsclean.authContext.integration.AbstractAuthJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaIdentityRepositoryIT extends AbstractAuthJpaIntegrationTest {

    private static final UUID ID       = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PROVIDER = "google";
    private static final String PROVIDER_USER_ID = "google-sub-123456";
    private static final String EMAIL = "user@example.test";
    private static final Instant CREATED_AT = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant LAST_AUTH_AT = Instant.parse("2024-01-01T11:00:00Z");

    @Autowired
    private IdentityRepository identityRepository;       // port DDD

    @Autowired
    private SpringIdentityRepository springIdentityRepository; // Spring Data brut

    @Test
    void repositories_are_injected() {
        assertThat(identityRepository).isNotNull();
        assertThat(springIdentityRepository).isNotNull();
    }

    @Test
    void can_save_identity() {
        var identity = new Identity(
                ID,
                USER_ID,
                PROVIDER,
                PROVIDER_USER_ID,
                EMAIL,
                CREATED_AT,
                LAST_AUTH_AT
        );

        identityRepository.save(identity);

        assertThat(springIdentityRepository.findAll()).containsExactly(
                new IdentityJpaEntity(
                        ID,
                        USER_ID,
                        PROVIDER,
                        PROVIDER_USER_ID,
                        EMAIL,
                        CREATED_AT,
                        LAST_AUTH_AT
                )
        );
    }

    @Test
    void can_find_by_provider_and_providerUserId() {
        // seed database
        springIdentityRepository.save(
                new IdentityJpaEntity(
                        ID,
                        USER_ID,
                        PROVIDER,
                        PROVIDER_USER_ID,
                        EMAIL,
                        CREATED_AT,
                        LAST_AUTH_AT
                )
        );

        var loaded = identityRepository
                .findByProviderAndProviderUserId(PROVIDER, PROVIDER_USER_ID)
                .orElseThrow();

        assertThat(loaded.userId()).isEqualTo(USER_ID);
        assertThat(loaded.provider()).isEqualTo(PROVIDER);
        assertThat(loaded.providerUserId()).isEqualTo(PROVIDER_USER_ID);
        assertThat(loaded.email()).isEqualTo(EMAIL);
    }

    @Test
    void can_list_by_userId() {
        var otherUserId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        springIdentityRepository.saveAll(List.of(
                new IdentityJpaEntity(
                        ID,
                        USER_ID,
                        PROVIDER,
                        PROVIDER_USER_ID,
                        EMAIL,
                        CREATED_AT,
                        LAST_AUTH_AT
                ),
                new IdentityJpaEntity(
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        otherUserId,
                        PROVIDER,
                        "other-sub",
                        "other@example.test",
                        CREATED_AT,
                        LAST_AUTH_AT
                )
        ));

        var identities = identityRepository.listByUserId(USER_ID);
        assertThat(identities).hasSize(1);
        var identity = identities.getFirst();
        assertThat(identity.userId()).isEqualTo(USER_ID);
        assertThat(identity.providerUserId()).isEqualTo(PROVIDER_USER_ID);
    }
}

package com.nm.fragmentsclean.authContext.unit;

import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeJwtTokenGenerator;
import com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake.FakeOAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommand;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommandHandler;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionResult;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RefreshSessionCommandHandlerTest {

    private final String PROVIDER = "google";
    private final String ID_TOKEN = "dummy-id-token";
    private final String ACCESS_TOKEN = "dummy-access-token-from-provider";
    private final String REFRESH_TOKEN = "dummy-refresh-token-from-provider";
    private final List<String> SCOPES = List.of("openid", "profile", "email");

    DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();
    FakeIdentityRepository identityRepository = new FakeIdentityRepository();
    FakeUserRepository userRepository = new FakeUserRepository();
    FakeJwtTokenGenerator jwtTokenGenerator = new FakeJwtTokenGenerator();
    FakeOAuthIdTokenVerifier oAuthIdTokenVerifier = new FakeOAuthIdTokenVerifier();

    RefreshSessionCommandHandler handler;

    @BeforeEach
    void setup() {
        // On fixe le temps serveur
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:00:00Z");

        handler = new RefreshSessionCommandHandler(
                oAuthIdTokenVerifier,
                identityRepository,
                userRepository,
                jwtTokenGenerator,
                dateTimeProvider
        );
    }

    @Test
    void should_create_user_and_identity_and_return_snapshot_and_tokens() {
        // GIVEN
        Instant clientEstablishedAt = Instant.parse("2023-10-01T09:55:00Z");

        RefreshSessionCommand command = new RefreshSessionCommand(
                PROVIDER,
                ACCESS_TOKEN,
                ID_TOKEN,
                REFRESH_TOKEN,
                /* expiresAt provider */ dateTimeProvider.instantOfNow.plusSeconds(3000).getEpochSecond(),
                /* issuedAt provider */ dateTimeProvider.instantOfNow.minusSeconds(600).getEpochSecond(),
                SCOPES,
                /* existingUserId */ null,
                clientEstablishedAt
        );

        // WHEN
        RefreshSessionResult result = handler.execute(command);

        // THEN : état côté repositories

        // userRepository doit avoir persisté exactement 1 user
        var users = userRepository.allUsers();
        assertThat(users).hasSize(1);
        AppUser user = users.getFirst();

        // identityRepository doit avoir 1 identity liée à ce user
        var identities = identityRepository.allIdentities();
        assertThat(identities).hasSize(1);
        Identity identity = identities.getFirst();
        assertThat(identity.userId()).isEqualTo(user.id());
        assertThat(identity.provider()).isEqualTo(PROVIDER);
        assertThat(identity.providerUserId()).startsWith("fake-user-"); // vient du FakeOAuthIdTokenVerifier
        assertThat(identity.email()).endsWith("@example.test");
        assertThat(identity.lastAuthAt()).isEqualTo(dateTimeProvider.instantOfNow);

        // THEN : tokens générés

        AppSessionTokens tokens = result.tokens();
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).startsWith("fake-access-");
        assertThat(tokens.idToken()).startsWith("fake-id-");
        assertThat(tokens.refreshToken()).startsWith("fake-refresh-");
        assertThat(tokens.issuedAt()).isEqualTo(dateTimeProvider.instantOfNow.getEpochSecond());
        assertThat(tokens.expiresAt()).isEqualTo(dateTimeProvider.instantOfNow.getEpochSecond() + 3600);
        assertThat(tokens.tokenType()).isEqualTo("Bearer");
        assertThat(tokens.scope()).isEqualTo("openid profile email");

        // THEN : RefreshSessionResult (read model)

        assertThat(result.provider()).isEqualTo(PROVIDER);
        assertThat(result.scopes()).containsExactlyInAnyOrderElementsOf(SCOPES);

        AppUserSnapshot snapshot = result.user();
        assertThat(snapshot.id()).isEqualTo(user.id().toString());
        assertThat(snapshot.displayName()).isEqualTo(user.displayName());
        assertThat(snapshot.avatarUrl()).isEqualTo(user.avatarUrl());
        assertThat(snapshot.roles()).contains("user");
        assertThat(snapshot.identities()).hasSize(1);

        var identitySnapshot = snapshot.identities().getFirst();
        assertThat(identitySnapshot.provider()).isEqualTo(identity.provider());
        assertThat(identitySnapshot.providerUserId()).isEqualTo(identity.providerUserId());
        assertThat(identitySnapshot.email()).isEqualTo(identity.email());
    }

    // ----------------------------------------------------------------------
    // FAKES spécifiques au test (in-memory, comme FakeCommentRepository)
    // ----------------------------------------------------------------------

    static class FakeIdentityRepository implements IdentityRepository {
        private final List<Identity> store = new ArrayList<>();

        @Override
        public Optional<Identity> findByProviderAndProviderUserId(String provider, String providerUserId) {
            return store.stream()
                    .filter(i -> Objects.equals(i.provider(), provider)
                            && Objects.equals(i.providerUserId(), providerUserId))
                    .findFirst();
        }

        @Override
        public List<Identity> listByUserId(UUID userId) {
            return store.stream()
                    .filter(i -> Objects.equals(i.userId(), userId))
                    .collect(Collectors.toList());
        }

        @Override
        public Identity save(Identity identity) {
            store.removeIf(i -> Objects.equals(i.id(), identity.id()));
            store.add(identity);
            return identity;
        }

        public List<Identity> allIdentities() {
            return List.copyOf(store);
        }
    }

    static class FakeUserRepository implements UserRepository {
        private final List<AppUser> store = new ArrayList<>();

        @Override
        public Optional<AppUser> findById(UUID userId) {
            return store.stream()
                    .filter(u -> Objects.equals(u.id(), userId))
                    .findFirst();
        }

        @Override
        public AppUser save(AppUser user) {
            store.removeIf(u -> Objects.equals(u.id(), user.id()));
            store.add(user);
            return user;
        }

        public List<AppUser> allUsers() {
            return List.copyOf(store);
        }
    }
}

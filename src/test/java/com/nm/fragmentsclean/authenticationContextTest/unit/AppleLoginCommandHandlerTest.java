package com.nm.fragmentsclean.authenticationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AppleLoginCommandHandlerTest {
  @Test
  void
      creates_an_apple_identity_persists_the_provider_refresh_token_and_returns_a_fragments_session() {
    var users = new Users();
    var credentials = new Credentials();
    var events = new FakeDomainEventPublisher();
    AppleAuthService apple =
        new AppleAuthService() {
          @Override
          public AppleUserInfo authenticate(String token, String code, String name) {
            return new AppleUserInfo(
                "apple-sub", "relay@privaterelay.appleid.com", true, name, "apple-refresh");
          }

          @Override
          public void revoke(String token) {}
        };
    var completion =
        new CompleteAppleLogin(
            users,
            credentials,
            (id, claims) ->
                new TokenService.TokenPair(
                    "access", RefreshToken.createNew(id, "refresh", Instant.now().plusSeconds(60))),
            new DeterministicDateTimeProvider(),
            user ->
                new JwtClaims(
                    user.id().toString(),
                    user.email(),
                    Set.of(),
                    Set.of(),
                    Instant.now(),
                    Instant.now().plusSeconds(60)),
            events);
    var handler = new AppleLoginCommandHandler(apple, completion);

    var result = handler.execute(new AppleLoginCommand("identity", "code", "Nicolas"));

    assertThat(users.user.provider()).isEqualTo(AuthProvider.APPLE);
    assertThat(credentials.token).isEqualTo("apple-refresh");
    assertThat(result.accessToken()).isEqualTo("access");
    assertThat(events.published).singleElement().isInstanceOf(AuthUserCreatedEvent.class);
  }

  private static final class Users implements AuthUserRepository {
    AuthUser user;

    @Override
    public Optional<AuthUser> findByProviderAndProviderUserId(AuthProvider p, String id) {
      return Optional.ofNullable(user)
          .filter(x -> x.provider() == p && x.providerUserId().equals(id));
    }

    @Override
    public AuthUser save(AuthUser value) {
      user = value;
      return value;
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
      return Optional.ofNullable(user).filter(x -> x.id().equals(id));
    }
  }

  private static final class Credentials implements ProviderCredentialRepository {
    String token;

    @Override
    public void save(UUID userId, AuthProvider provider, String refreshToken) {
      token = refreshToken;
    }

    @Override
    public Optional<String> findRefreshToken(UUID userId, AuthProvider provider) {
      return Optional.ofNullable(token);
    }

    @Override
    public void delete(UUID userId, AuthProvider provider) {
      token = null;
    }
  }
}

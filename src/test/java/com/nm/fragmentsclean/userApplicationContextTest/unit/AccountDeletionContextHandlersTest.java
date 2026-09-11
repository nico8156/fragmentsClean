package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.ProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.CompleteAuthenticationAccountDataErasure;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.EraseAuthenticationAccountData;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.SocialAccountDataEraser;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.SocialAccountDataErasedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.EraseSocialAccountData;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketAccountDataEraser;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAccountDataErasedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.EraseTicketAccountData;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AccountDeletionContextHandlersTest {
  private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final AppUserDeletionRequestedIntegrationEvent REQUEST =
      new AppUserDeletionRequestedIntegrationEvent(
          UUID.randomUUID(),
          REQUEST_ID,
          USER_ID,
          USER_ID,
          1,
          Instant.parse("2026-09-11T10:00:00Z"));

  @Test
  void authentication_erases_identity_revokes_every_refresh_token_and_acknowledges() {
    var authUser =
        new AuthUser(
            USER_ID,
            AuthProvider.GOOGLE,
            "google-sub",
            "nico@example.test",
            true,
            "Nicolas",
            "avatar",
            Instant.now());
    var users = new FakeAuthUsers(authUser);
    var tokens =
        new FakeTokens(
            List.of(
                RefreshToken.createNew(USER_ID, "one", Instant.now().plusSeconds(60)),
                RefreshToken.createNew(USER_ID, "two", Instant.now().plusSeconds(60))));
    var events = new FakeDomainEventPublisher();

    var clock = new DeterministicDateTimeProvider();
    var credentials = new FakeCredentials();
    var completion =
        new CompleteAuthenticationAccountDataErasure(users, tokens, credentials, events, clock);
    new EraseAuthenticationAccountData(users, credentials, new FakeApple(), completion)
        .handle(REQUEST);

    assertThat(users.user.lifecycleStatus()).isEqualTo(AuthUserLifecycleStatus.DELETED);
    assertThat(users.user.email()).endsWith("@invalid.local");
    assertThat(tokens.tokens).allMatch(RefreshToken::revoked);
    assertThat(events.published)
        .singleElement()
        .isInstanceOf(AuthenticationAccountDataErasedEvent.class);
  }

  @Test
  void social_and_ticket_erase_only_through_their_own_ports_then_acknowledge() {
    var social = new RecordingSocialEraser();
    var ticket = new RecordingTicketEraser();
    var events = new FakeDomainEventPublisher();
    var clock = new DeterministicDateTimeProvider();

    new EraseSocialAccountData(social, events, clock).handle(REQUEST);
    new EraseTicketAccountData(ticket, events, clock).handle(REQUEST);

    assertThat(social.erased).isEqualTo(USER_ID);
    assertThat(ticket.erased).isEqualTo(USER_ID);
    assertThat(events.published)
        .anyMatch(SocialAccountDataErasedEvent.class::isInstance)
        .anyMatch(TicketAccountDataErasedEvent.class::isInstance);
  }

  private static final class FakeAuthUsers implements AuthUserRepository {
    private AuthUser user;

    FakeAuthUsers(AuthUser user) {
      this.user = user;
    }

    @Override
    public Optional<AuthUser> findByProviderAndProviderUserId(AuthProvider p, String id) {
      return Optional.empty();
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

  private static final class FakeTokens implements RefreshTokenRepository {
    private final List<RefreshToken> tokens;

    FakeTokens(List<RefreshToken> tokens) {
      this.tokens = new ArrayList<>(tokens);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
      return tokens.stream().filter(x -> x.token().equals(token)).findFirst();
    }

    @Override
    public RefreshToken save(RefreshToken token) {
      return token;
    }

    @Override
    public List<RefreshToken> findAllByUserId(UUID userId) {
      return tokens.stream().filter(x -> x.userId().equals(userId)).toList();
    }
  }

  private static final class FakeCredentials implements ProviderCredentialRepository {
    @Override
    public void save(UUID userId, AuthProvider provider, String token) {}

    @Override
    public Optional<String> findRefreshToken(UUID userId, AuthProvider provider) {
      return Optional.empty();
    }

    @Override
    public void delete(UUID userId, AuthProvider provider) {}
  }

  private static final class FakeApple implements AppleAuthService {
    @Override
    public AppleUserInfo authenticate(String identityToken, String code, String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void revoke(String token) {}
  }

  private static final class RecordingSocialEraser implements SocialAccountDataEraser {
    UUID erased;

    public void erase(UUID id) {
      erased = id;
    }
  }

  private static final class RecordingTicketEraser implements TicketAccountDataEraser {
    UUID erased;

    public void erase(UUID id) {
      erased = id;
    }
  }
}

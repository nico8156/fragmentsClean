package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.util.UUID;

public class CompleteAuthenticationAccountDataErasure {
  private final AuthUserRepository users;
  private final RefreshTokenRepository tokens;
  private final ProviderCredentialRepository credentials;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;
  private final AccountErasureBarrier barrier;

  public CompleteAuthenticationAccountDataErasure(
      AuthUserRepository users,
      RefreshTokenRepository tokens,
      ProviderCredentialRepository credentials,
      DomainEventPublisher events,
      DateTimeProvider clock,
      AccountErasureBarrier barrier) {
    this.users = users;
    this.tokens = tokens;
    this.credentials = credentials;
    this.events = events;
    this.clock = clock;
    this.barrier = barrier;
  }

  public void execute(AppUserDeletionRequestedIntegrationEvent request) {
    var now = clock.now();
    barrier.erase(AccountErasureBarrier.Scope.AUTHENTICATION, request.userId(), request.requestId(), now, () -> {
      var user = users.findById(request.authUserId());
      user.ifPresent(
          value -> {
            if (value.erasePersonalData(now)) users.save(value);
          });
      tokens
          .findAllByUserId(request.authUserId())
          .forEach(
              token -> {
                if (!token.revoked()) {
                  token.revoke();
                  tokens.save(token);
                }
              });
      user.ifPresent(value -> credentials.delete(value.id(), value.provider()));
      events.publish(
          new AuthenticationAccountDataErasedEvent(
              UUID.randomUUID(), request.requestId(), request.userId(), "AUTHENTICATION", now));
    });
  }
}

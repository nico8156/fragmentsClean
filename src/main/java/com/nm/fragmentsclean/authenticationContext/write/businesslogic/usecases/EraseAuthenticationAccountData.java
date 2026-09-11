package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.ProviderCredentialRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;

public final class EraseAuthenticationAccountData {
  private final AuthUserRepository users;
  private final ProviderCredentialRepository credentials;
  private final AppleAuthService apple;
  private final CompleteAuthenticationAccountDataErasure completion;

  public EraseAuthenticationAccountData(
      AuthUserRepository users,
      ProviderCredentialRepository credentials,
      AppleAuthService apple,
      CompleteAuthenticationAccountDataErasure completion) {
    this.users = users;
    this.credentials = credentials;
    this.apple = apple;
    this.completion = completion;
  }

  public void handle(AppUserDeletionRequestedIntegrationEvent request) {
    users
        .findById(request.authUserId())
        .filter(user -> user.provider() == AuthProvider.APPLE)
        .flatMap(user -> credentials.findRefreshToken(user.id(), user.provider()))
        .ifPresent(apple::revoke);
    completion.execute(request);
  }
}

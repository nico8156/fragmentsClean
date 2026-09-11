package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;

public final class AppleLoginCommandHandler
    implements CommandHandlerWithResult<AppleLoginCommand, AppleLoginResult> {
  private final AppleAuthService apple;
  private final CompleteAppleLogin completion;

  public AppleLoginCommandHandler(AppleAuthService apple, CompleteAppleLogin completion) {
    this.apple = apple;
    this.completion = completion;
  }

  @Override
  public AppleLoginResult execute(AppleLoginCommand command) {
    var profile =
        apple.authenticate(
            command.identityToken(), command.authorizationCode(), command.displayName());
    return completion.execute(profile);
  }
}

package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("auth_test")
public final class FakeAppleAuthService implements AppleAuthService {
  @Override
  public AppleUserInfo authenticate(
      String identityToken, String authorizationCode, String displayName) {
    return new AppleUserInfo(
        "fake-apple-sub-" + authorizationCode,
        "apple@example.test",
        true,
        displayName,
        "apple-refresh-" + authorizationCode);
  }

  @Override
  public void revoke(String token) {}
}

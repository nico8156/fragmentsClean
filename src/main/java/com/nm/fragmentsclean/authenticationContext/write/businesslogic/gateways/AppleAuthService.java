package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

public interface AppleAuthService {
  AppleUserInfo authenticate(String identityToken, String authorizationCode, String displayName);

  void revoke(String providerRefreshToken);

  record AppleUserInfo(
      String sub,
      String email,
      boolean emailVerified,
      String displayName,
      String providerRefreshToken) {}
}

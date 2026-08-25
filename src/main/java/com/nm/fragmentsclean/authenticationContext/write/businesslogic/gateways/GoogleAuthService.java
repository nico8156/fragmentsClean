package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

public interface GoogleAuthService {

    GoogleUserInfo exchangeMobileAuthorizationCodeForUser(
            String authorizationCode,
            String codeVerifier,
            String redirectUri);

    default GoogleUserInfo exchangeStudioAuthorizationCodeForUser(
            String authorizationCode,
            String codeVerifier,
            String redirectUri) {
        return exchangeMobileAuthorizationCodeForUser(authorizationCode, codeVerifier, redirectUri);
    }

    record GoogleUserInfo(
            String sub,
            String email,
            boolean emailVerified,
            String name,
            String pictureUrl
    ) {}
}

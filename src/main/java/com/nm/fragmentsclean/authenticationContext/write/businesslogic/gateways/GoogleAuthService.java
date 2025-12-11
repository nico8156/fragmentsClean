package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

public interface GoogleAuthService {

    GoogleUserInfo exchangeCodeForUser(String authorizationCode);


    record GoogleUserInfo(
            String sub,
            String email,
            boolean emailVerified,
            String name,
            String pictureUrl
    ) {}
}

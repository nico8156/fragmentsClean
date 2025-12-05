package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

public record RefreshTokenResult(
        String accessToken,
        String refreshToken
) {}

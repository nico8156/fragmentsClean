package com.nm.fragmentsclean.authContext.businesslogic.models;

public record AppSessionTokens(
        String accessToken,
        String idToken,
        String refreshToken,
        long expiresAt,
        Long issuedAt,
        String tokenType,
        String scope
) {
}

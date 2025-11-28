package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto;

public record AuthTokensDto(
        String accessToken,
        String idToken,
        String refreshToken,
        long expiresAt,
        Long issuedAt,
        String tokenType,
        String scope
) {}

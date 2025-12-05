package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

public record RefreshTokenResponseDto(
        String accessToken,
        String refreshToken
) {
}

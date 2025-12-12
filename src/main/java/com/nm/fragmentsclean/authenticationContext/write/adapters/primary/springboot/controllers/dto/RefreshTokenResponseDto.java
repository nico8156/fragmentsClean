package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto;

public record RefreshTokenResponseDto(
        String accessToken,
        String refreshToken
) {
}

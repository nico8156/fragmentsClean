package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import java.util.UUID;

public record GoogleLoginResponseDto(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String displayName,
            String email,
            String avatarUrl
    ) {}
}

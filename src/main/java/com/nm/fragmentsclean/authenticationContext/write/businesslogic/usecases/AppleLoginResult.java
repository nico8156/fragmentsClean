package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import java.util.UUID;

public record AppleLoginResult(
    String accessToken,
    String refreshToken,
    UUID userId,
    String displayName,
    String email,
    String avatarUrl) {}

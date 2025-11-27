package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto;

import java.util.List;

public record RefreshSessionRequestDto(
        String provider,          // "google"
        AuthTokensDto tokens,
        String userId,            // peut être null pour first login
        List<String> scopes,
        long establishedAt        // epoch ms côté client
) {}

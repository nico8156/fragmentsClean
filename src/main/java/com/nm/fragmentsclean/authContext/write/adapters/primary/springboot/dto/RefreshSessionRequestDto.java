package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto;

import java.time.Instant;
import java.util.List;

public record RefreshSessionRequestDto(
        String provider,
        String idToken,
        String accessToken,
        List<String> scopes,
        Instant sessionEstablishedAt,
        Instant sessionExpiresAt
) {}

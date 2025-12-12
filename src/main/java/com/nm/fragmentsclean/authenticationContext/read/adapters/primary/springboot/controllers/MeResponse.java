package com.nm.fragmentsclean.authenticationContext.read.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String displayName,
        Instant issuedAt,
        Instant expiresAt,
        Instant serverTime
) {
}

package com.nm.fragmentsclean.authenticationContext.read.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        Instant issuedAt,
        Instant expiresAt,
        Instant serverTime
) {
}

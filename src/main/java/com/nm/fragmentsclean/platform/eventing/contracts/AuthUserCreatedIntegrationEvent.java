package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AuthUserCreatedIntegrationEvent(
        UUID eventId,
        UUID authUserId,
        String provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl,
        Instant occurredAt
) {
}

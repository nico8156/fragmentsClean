package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AppUserCreatedIntegrationEvent(
        UUID eventId,
        UUID userId,
        UUID authUserId,
        String displayName,
        String avatarUrl,
        long version,
        Instant occurredAt
) {
}

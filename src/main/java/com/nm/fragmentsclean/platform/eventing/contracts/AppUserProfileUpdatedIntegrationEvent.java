package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AppUserProfileUpdatedIntegrationEvent(
        UUID eventId,
        UUID userId,
        String displayName,
        String avatarUrl,
        long version,
        Instant occurredAt
) {
}

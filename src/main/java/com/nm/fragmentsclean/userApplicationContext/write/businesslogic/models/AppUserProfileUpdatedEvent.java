package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AppUserProfileUpdatedEvent(
        UUID eventId,
        UUID userId,
        String displayName,
        String avatarUrl,
        long version,
        Instant occurredAt
) implements DomainEvent {
}

package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AppUserCreatedEvent(
        UUID eventId,
        UUID userId,      // IMPORTANT: garder userId pour matcher OutboxDomainEventPublisher
        UUID authUserId,
        String displayName,
        String avatarUrl,
        long version,
        Instant occurredAt
) implements DomainEvent {

    public static AppUserCreatedEvent of(AppUser user, Instant occurredAt) {
        return new AppUserCreatedEvent(
                UUID.randomUUID(),
                user.id(),
                user.authUserId(),
                user.displayName(),
                user.avatarUrl(),
                user.version(),
                occurredAt
        );
    }
}

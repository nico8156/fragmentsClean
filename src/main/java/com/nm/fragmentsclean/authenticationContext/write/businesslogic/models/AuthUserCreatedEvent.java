package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AuthUserCreatedEvent(
        UUID eventId,
        UUID authUserId,
        AuthProvider provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        Instant occurredAt
) implements DomainEvent {

    public static AuthUserCreatedEvent of(AuthUser authUser, Instant occurredAt) {
        return new AuthUserCreatedEvent(
                UUID.randomUUID(),
                authUser.id(),
                authUser.provider(),
                authUser.providerUserId(),
                authUser.email(),
                authUser.emailVerified(),
                occurredAt
        );
    }
}

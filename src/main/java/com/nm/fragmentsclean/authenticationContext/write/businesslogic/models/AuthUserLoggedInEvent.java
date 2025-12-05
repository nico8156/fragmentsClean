package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AuthUserLoggedInEvent(
        UUID eventId,
        UUID authUserId,
        AuthProvider provider,
        String providerUserId,
        Instant occurredAt
) implements DomainEvent {

    public static AuthUserLoggedInEvent of(AuthUser authUser, Instant occurredAt) {
        return new AuthUserLoggedInEvent(
                UUID.randomUUID(),
                authUser.id(),
                authUser.provider(),
                authUser.providerUserId(),
                occurredAt
        );
    }
}

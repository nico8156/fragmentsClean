package com.nm.fragmentsclean.authContext.write.businesslogic.models.events;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserAuthenticatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String provider
) implements DomainEvent {
}

package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AuthenticationAccountDataErasedEvent(
    UUID eventId, UUID requestId, UUID userId, String context, Instant occurredAt)
    implements DomainEvent {}

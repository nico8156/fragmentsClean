package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AppUserDeletionRequestedEvent(
    UUID eventId, UUID requestId, UUID userId, UUID authUserId, long version, Instant occurredAt)
    implements DomainEvent {}

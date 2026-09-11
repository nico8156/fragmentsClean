package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record SocialAccountDataErasedEvent(
    UUID eventId, UUID requestId, UUID userId, String context, Instant occurredAt)
    implements DomainEvent {}

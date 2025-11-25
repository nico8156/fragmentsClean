package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record LikeSetEvent(
        UUID eventId,
        UUID commandId,
        UUID likeId,
        UUID userId,
        UUID targetId,
        boolean active,
        Instant occurredAt
) implements DomainEvent {}

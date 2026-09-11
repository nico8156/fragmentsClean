package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record UserBlockChangedEvent(
        UUID eventId,
        UUID commandId,
        UUID blockId,
        UUID blockerId,
        UUID blockedUserId,
        boolean active,
        long version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent { }

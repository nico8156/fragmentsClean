package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record CommentDeletedEvent(
        UUID eventId,
        UUID commandId,
        UUID commentId,
        UUID targetId,
        UUID authorId,
        ModerationStatus moderation,
        Instant deletedAt,
        long version,
        Instant occurredAt,
        Instant clientAt
)implements DomainEvent {
}

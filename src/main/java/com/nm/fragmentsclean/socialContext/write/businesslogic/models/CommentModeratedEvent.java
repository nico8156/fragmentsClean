package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CommentModeratedEvent(
        UUID eventId,
        UUID commandId,
        UUID actionId,
        UUID reportId,
        UUID commentId,
        UUID targetId,
        UUID authorId,
        UUID operatorId,
        ModerationStatus moderation,
        ReportStatus reportStatus,
        String reason,
        long version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent { }

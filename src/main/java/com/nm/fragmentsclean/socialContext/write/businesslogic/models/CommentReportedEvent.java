package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record CommentReportedEvent(
        UUID eventId,
        UUID commandId,
        UUID reportId,
        UUID commentId,
        UUID targetId,
        UUID authorId,
        UUID reporterId,
        ReportReason reason,
        String details,
        ReportStatus status,
        long version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent { }

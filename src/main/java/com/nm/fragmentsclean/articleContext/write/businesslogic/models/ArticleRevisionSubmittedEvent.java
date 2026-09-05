package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ArticleRevisionSubmittedEvent(
        UUID eventId,
        UUID commandId,
        UUID articleId,
        UUID revisionId,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent {
}

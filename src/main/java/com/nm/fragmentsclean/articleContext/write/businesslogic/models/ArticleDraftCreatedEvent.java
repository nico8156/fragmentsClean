package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ArticleDraftCreatedEvent(
        UUID eventId,
        UUID commandId,
        UUID articleId,
        UUID revisionId,
        String slug,
        String locale,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent {
}

package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleCreatedEvent(
        UUID eventId,
        UUID commandId,
        UUID articleId,
        String slug,
        String locale,
        UUID authorId,
        String authorName,
        String title,
        String intro,
        String blocksJson,
        String conclusion,
        String coverUrl,
        Integer coverWidth,
        Integer coverHeight,
        String coverAlt,
        List<String> tags,
        Integer readingTimeMin,
        List<UUID> coffeeIds,
        ArticleStatus status,
        long version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent {
}

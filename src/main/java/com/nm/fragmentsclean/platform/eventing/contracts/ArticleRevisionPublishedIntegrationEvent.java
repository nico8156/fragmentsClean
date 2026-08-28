package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record ArticleRevisionPublishedIntegrationEvent(
        UUID eventId,
        UUID commandId,
        UUID articleId,
        UUID revisionId,
        long version,
        Instant occurredAt,
        Instant clientAt
) {
}

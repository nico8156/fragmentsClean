package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record ArticleGenerationRequestedIntegrationEvent(
        UUID eventId, UUID commandId, UUID sagaId, UUID articleId, UUID revisionId,
        String theme, String locale, String trigger, long version,
        Instant occurredAt, Instant clientAt
) { }

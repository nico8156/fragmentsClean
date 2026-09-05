package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

/** Primitive lifecycle contract shared by article revision workflow events. */
public record ArticleWorkflowIntegrationEvent(
        UUID eventId, UUID commandId, UUID sagaId, UUID articleId, UUID revisionId,
        String slug, String locale, Instant occurredAt, Instant clientAt) { }

package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Signals that the validated provider artifact is ready for materialisation into a revision. */
public record ArticleGenerationCompletedEvent(
        UUID eventId, UUID sagaId, UUID articleId, UUID revisionId,
        UUID runId, String provider, String providerResponseId, String model,
        String schemaVersion, long sagaVersion, Instant occurredAt
) implements DomainEvent { }

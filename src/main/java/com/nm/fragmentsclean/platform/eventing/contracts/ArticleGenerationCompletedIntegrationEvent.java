package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record ArticleGenerationCompletedIntegrationEvent(
        UUID eventId, UUID sagaId, UUID articleId, UUID revisionId,
        UUID runId, String provider, String providerResponseId, String model,
        String schemaVersion, long sagaVersion, Instant occurredAt
) { }

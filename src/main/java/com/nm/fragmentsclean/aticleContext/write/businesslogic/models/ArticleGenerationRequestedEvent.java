package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ArticleGenerationRequestedEvent(
        UUID eventId, UUID commandId, UUID sagaId, UUID articleId, UUID revisionId,
        String theme, String locale, ArticleAuthoringTrigger trigger,
        long version, Instant occurredAt, Instant clientAt
) implements DomainEvent { }

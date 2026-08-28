package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ArticleArchivedEvent(UUID eventId, UUID commandId, UUID articleId, UUID revisionId,
                                   long version, Instant occurredAt, Instant clientAt)
        implements DomainEvent { }

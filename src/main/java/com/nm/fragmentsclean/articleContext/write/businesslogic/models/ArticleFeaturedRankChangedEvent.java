package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ArticleFeaturedRankChangedEvent(UUID eventId, UUID commandId, UUID articleId,
                                              Integer featuredRank, long version,
                                              Instant occurredAt, Instant clientAt) implements DomainEvent { }

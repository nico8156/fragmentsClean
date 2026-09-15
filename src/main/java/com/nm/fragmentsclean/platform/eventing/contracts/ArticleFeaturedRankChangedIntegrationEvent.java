package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record ArticleFeaturedRankChangedIntegrationEvent(UUID eventId, UUID commandId, UUID articleId,
                                                         Integer featuredRank, long version,
                                                         Instant occurredAt, Instant clientAt) { }

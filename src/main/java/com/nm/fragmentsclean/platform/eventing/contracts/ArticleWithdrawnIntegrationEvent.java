package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record ArticleWithdrawnIntegrationEvent(UUID eventId, UUID commandId, UUID articleId,
                                               UUID publishedRevisionId, UUID draftRevisionId,
                                               long version, Instant occurredAt, Instant clientAt) { }

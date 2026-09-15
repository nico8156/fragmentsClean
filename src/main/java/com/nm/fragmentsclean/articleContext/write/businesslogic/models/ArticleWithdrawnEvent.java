package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ArticleWithdrawnEvent(UUID eventId, UUID commandId, UUID articleId,
                                    UUID publishedRevisionId, UUID draftRevisionId, long version,
                                    Instant occurredAt, Instant clientAt) implements DomainEvent { }

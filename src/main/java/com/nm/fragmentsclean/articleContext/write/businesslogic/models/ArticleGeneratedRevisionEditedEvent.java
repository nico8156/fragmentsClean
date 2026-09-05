package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public record ArticleGeneratedRevisionEditedEvent(
		UUID eventId,
		UUID commandId,
		UUID sagaId,
		UUID articleId,
		UUID revisionId,
		Instant occurredAt,
		Instant clientAt) implements DomainEvent {
}

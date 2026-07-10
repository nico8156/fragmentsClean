package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.UUID;

public record StudioArticleDocument(
		UUID articleId,
		String status,
		String payloadJson,
		Instant createdAt,
		Instant updatedAt,
		Instant publishedAt,
		Instant deletedAt,
		UUID lastCommandId) {
}

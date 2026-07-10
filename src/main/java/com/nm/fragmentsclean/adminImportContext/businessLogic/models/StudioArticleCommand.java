package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioArticleCommand(
		UUID commandId,
		Instant clientAt,
		UUID articleId,
		String slug,
		String locale,
		UUID authorId,
		String authorName,
		String title,
		String intro,
		String blocksJson,
		String conclusion,
		String coverUrl,
		Integer coverWidth,
		Integer coverHeight,
		String coverAlt,
		List<String> tags,
		Integer readingTimeMin,
		List<UUID> coffeeIds
) {
}

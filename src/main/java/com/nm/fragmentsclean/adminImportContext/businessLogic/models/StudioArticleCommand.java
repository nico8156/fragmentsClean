package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudioArticleCommand(
		UUID commandId,
		Instant clientAt,
		UUID articleId,
		UUID revisionId,
		String slug,
		String locale,
		UUID authorId,
		String authorName,
		String title,
		String intro,
		List<StudioArticleBlock> blocks,
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

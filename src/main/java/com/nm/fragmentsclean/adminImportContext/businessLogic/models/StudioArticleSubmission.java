package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.List;
import java.util.UUID;

public record StudioArticleSubmission(
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
		StudioArticleImageRef cover,
		List<String> tags,
		Integer readingTimeMin,
		List<UUID> coffeeIds) {
}

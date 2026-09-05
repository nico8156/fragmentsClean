package com.nm.fragmentsclean.articleContext.read;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetArticleGenerationReview(
		UUID sagaId,
		UUID articleId,
		UUID revisionId,
		String subject,
		String state,
		int attempts,
		Instant updatedAt,
		Revision revision) {
	public record Revision(
			String title,
			String introduction,
			String conclusion,
			String coverReference,
			String coverUrl,
			Integer coverWidth,
			Integer coverHeight,
			String coverAlt,
			int readingTime,
			List<String> tags,
			List<Section> sections) {
	}

	public record Section(
			String heading,
			String paragraph,
			String imageReference,
			String imageUrl,
			Integer imageWidth,
			Integer imageHeight,
			String imageAlt) {
	}
}

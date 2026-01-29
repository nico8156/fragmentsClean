package com.nm.fragmentsclean.aticleContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record ArticleProjectionRow(
		UUID id,
		String slug,
		String locale,

		String title,
		String intro,
		String blocksJson,
		String conclusion,

		String coverJson, // nullable
		String tagsJson, // JSON string[]
		UUID authorId,
		String authorName,

		int readingTimeMin,

		Instant publishedAt,
		Instant updatedAt,

		long version,
		String status, // "published" | "draft" | "archived"

		String coffeeIdsJson // JSON string[] of UUIDs
) {
}

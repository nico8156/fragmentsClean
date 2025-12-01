package com.nm.fragmentsclean.aticleContext.read.projections;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleView(
        UUID id,
        String slug,
        String locale,                      // "fr-FR" / "en-US"

        String title,
        String intro,
        List<ArticleBlockView> blocks,
        String conclusion,

        ImageRefView cover,
        List<String> tags,

        UUID authorId,
        String authorName,

        Integer readingTimeMin,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt,

        Long version,
        String status,                      // "draft" | "published" | "archived"
        List<UUID> coffeeIds
) {
}

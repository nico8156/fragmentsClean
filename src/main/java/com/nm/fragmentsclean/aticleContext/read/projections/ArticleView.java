package com.nm.fragmentsclean.aticleContext.read.projections;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleView(
        UUID id,
        String slug,
        String locale,

        String title,
        String intro,
        List<ArticleBlockView> blocks,
        String conclusion,

        ImageRefView cover,       // peut être null
        List<String> tags,
        AuthorView author,

        int readingTimeMin,
        Instant publishedAt,
        Instant updatedAt,

        long version,
        String status,            // "published" | "draft" | "archived"
        List<UUID> coffeeIds
) {
}

package com.nm.fragmentsclean.articleContext.read;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Stable application read contract exposed to the Studio ACL. */
public record ArticleStudioDraftView(
        UUID articleId,
        UUID revisionId,
        String status,
        String slug,
        String locale,
        UUID authorId,
        String authorName,
        String title,
        String introduction,
        List<Section> sections,
        String conclusion,
        Image cover,
        List<String> tags,
        int readingTimeMin,
        List<UUID> coffeeIds,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {
    public record Section(String heading, String paragraph, Image image) { }
    public record Image(String storageReference, String url, int width, int height, String alt) { }
}

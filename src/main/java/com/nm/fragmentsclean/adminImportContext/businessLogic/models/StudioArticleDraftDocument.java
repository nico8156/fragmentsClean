package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.UUID;

public record StudioArticleDraftDocument(
        UUID articleId,
        UUID revisionId,
        String status,
        StudioArticleSubmission draft,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) { }

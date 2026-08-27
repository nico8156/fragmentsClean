package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationReview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationReviewPort;
import java.util.UUID;

public final class GetStudioArticleGenerationReview {
    private final ArticleGenerationReviewPort reviews;

    public GetStudioArticleGenerationReview(ArticleGenerationReviewPort reviews) {
        this.reviews = reviews;
    }

    public StudioArticleGenerationReview execute(UUID sagaId) {
        if (sagaId == null) throw new IllegalArgumentException("sagaId is required");
        return reviews.get(sagaId);
    }
}

package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationReview;
import java.util.UUID;

public interface ArticleGenerationReviewPort {
    StudioArticleGenerationReview get(UUID sagaId);
}

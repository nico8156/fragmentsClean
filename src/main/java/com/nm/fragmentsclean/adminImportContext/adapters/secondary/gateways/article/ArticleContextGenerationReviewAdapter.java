package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationReview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationReviewPort;
import com.nm.fragmentsclean.articleContext.read.GetArticleGenerationReview;
import com.nm.fragmentsclean.articleContext.read.GetArticleGenerationReviewQueryHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class ArticleContextGenerationReviewAdapter implements ArticleGenerationReviewPort {
    private final GetArticleGenerationReviewQueryHandler query;

    public ArticleContextGenerationReviewAdapter(GetArticleGenerationReviewQueryHandler query) {
        this.query = query;
    }

    @Override
    public StudioArticleGenerationReview get(UUID sagaId) {
        var source = query.handle(sagaId);
        return new StudioArticleGenerationReview(source.sagaId(), source.articleId(), source.revisionId(),
                source.subject(), source.state(), source.attempts(), source.updatedAt(), revision(source.revision()));
    }

    private static StudioArticleGenerationReview.Revision revision(GetArticleGenerationReview.Revision source) {
        if (source == null) return null;
        return new StudioArticleGenerationReview.Revision(source.title(), source.introduction(), source.conclusion(),
                source.coverReference(), source.coverUrl(), source.coverWidth(), source.coverHeight(), source.coverAlt(),
                source.readingTime(), source.tags(), source.sections().stream().map(section ->
                        new StudioArticleGenerationReview.Section(section.heading(), section.paragraph(),
                                section.imageReference(), section.imageUrl(), section.imageWidth(),
                                section.imageHeight(), section.imageAlt())).toList());
    }
}

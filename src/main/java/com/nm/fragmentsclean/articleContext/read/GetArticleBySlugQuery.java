package com.nm.fragmentsclean.articleContext.read;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record GetArticleBySlugQuery(
        String slug,
        String locale
) implements Query<ArticleView> {
}

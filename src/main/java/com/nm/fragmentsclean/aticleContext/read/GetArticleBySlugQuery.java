package com.nm.fragmentsclean.aticleContext.read;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Query;

public record GetArticleBySlugQuery(
        String slug,
        String locale
) implements Query<ArticleView> {
}

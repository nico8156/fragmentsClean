package com.nm.fragmentsclean.aticleContext.read;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleListView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record ListArticlesQuery(
        String locale,
        Integer limit,
        String cursor
) implements Query<ArticleListView> {
}

package com.nm.fragmentsclean.aticleContext.read.projections;

import java.util.List;

public record ArticleListView(
        List<ArticleView> items,
        String nextCursor,
        String prevCursor,
        String etag
) {
}

package com.nm.fragmentsclean.articleContext.read.projections;

public record ArticleBlockView(
        String heading,
        String paragraph,
        ImageRefView photo
) {
}
